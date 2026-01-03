package dev.matheuscruz.presentation;

import dev.langchain4j.data.image.Image;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordRepository;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.infra.ai.ImageAiService;
import dev.matheuscruz.infra.ai.TextAiService;
import dev.matheuscruz.infra.ai.data.AiOperations;
import dev.matheuscruz.infra.ai.data.RecognizedOperation;
import dev.matheuscruz.infra.ai.data.RecognizedTransaction;
import dev.matheuscruz.presentation.data.ImageRequest;
import dev.matheuscruz.presentation.data.MessageRequest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * We allow only whatsapp clients to send messages to this endpoint.
 * <p>
 * While we do not have everything implemented in an asynchronous way without the communication between whatsapp app and
 * our backend, we will block this endpoint to only whatsapp IP.
 * <p>
 * It is a block on network level, configured in the infrastructure layer.
 */
@Path("/api/messages")
public class MessageResource {

    final UserRepository userRepository;
    final TextAiService aiService;
    final ImageAiService imageAiService;
    final RecordRepository recordRepository;

    public MessageResource(TextAiService aiService, ImageAiService imageAiService, RecordRepository recordRepository,
            UserRepository userRepository) {
        this.aiService = aiService;
        this.imageAiService = imageAiService;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response message(@Valid MessageRequest req) {
        User user = userRepository.findByPhoneNumber(req.from()).orElseThrow(NotFoundException::new);
        return handleMessage(user, req.message());
    }

    @POST
    @Path("/image")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response image(@Valid ImageRequest req) {

        User user = userRepository.findByPhoneNumber(req.from()).orElseThrow(NotFoundException::new);
        RecognizedTransaction recognizedTransaction = imageAiService.handleTransactionImage(
                Image.builder().base64Data(req.base64()).mimeType(req.mimeType()).build(), req.text());

        if (recognizedTransaction.withError()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(recognizedTransaction).build();
        }

        QuarkusTransaction.requiringNew().run(() -> {

            Record record = new Record.Builder().userId(user.getId()).amount(recognizedTransaction.amount())
                    .description(recognizedTransaction.description()).transaction(recognizedTransaction.type())
                    .category(recognizedTransaction.category()).build();

            this.recordRepository.persist(record);
        });

        return Response.status(Response.Status.CREATED).entity(recognizedTransaction).build();
    }

    private Response handleMessage(User user, String message) {
        List<RecognizedOperation> response = aiService.handleMessage(message, user.getId()).all();
        return processOnlyAddTransaction(user, response);
    }

    private Response processOnlyAddTransaction(User user, List<RecognizedOperation> messages) {

        List<RecognizedTransaction> onlyAddTransaction = messages.stream()
                .filter(message -> AiOperations.ADD_TRANSACTION.equals(message.operation()))
                .map(RecognizedOperation::recognizedTransaction).toList();

        handleTransactions(onlyAddTransaction, user);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private void handleTransactions(List<RecognizedTransaction> transactions, User user) {
        QuarkusTransaction.requiringNew().run(() -> {
            Stream<Record> recordStream = transactions.stream().filter(Predicate.not(RecognizedTransaction::withError))
                    .map(recognizedTransaction -> new Record.Builder().userId(user.getId())
                            .amount(recognizedTransaction.amount()).description(recognizedTransaction.description())
                            .category(recognizedTransaction.category()).transaction(recognizedTransaction.type())
                            .build());
            recordRepository.persist(recordStream);
        });
    }
}
