package dev.matheuscruz.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.matheuscruz.domain.Categories;
import dev.matheuscruz.domain.Record;
import dev.matheuscruz.domain.RecordRepository;
import dev.matheuscruz.domain.Transactions;
import dev.matheuscruz.domain.User;
import dev.matheuscruz.domain.UserRepository;
import dev.matheuscruz.infra.ai.AudioAiService;
import dev.matheuscruz.infra.ai.ImageAiService;
import dev.matheuscruz.infra.ai.TextAiService;
import dev.matheuscruz.infra.ai.data.AiOperations;
import dev.matheuscruz.infra.ai.data.RecognizedOperation;
import dev.matheuscruz.infra.ai.data.RecognizedTransaction;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Path("/api/messages")
public class MessageResource {

    private final UserRepository userRepository;
    private final TextAiService aiService;
    private final ImageAiService imageAiService;
    private final AudioAiService audioAiService;
    private final RecordRepository recordRepository;

    public MessageResource(TextAiService aiService, ImageAiService imageAiService, AudioAiService audioAiService,
            RecordRepository recordRepository, ObjectMapper mapper, UserRepository userRepository) {
        this.aiService = aiService;
        this.imageAiService = imageAiService;
        this.audioAiService = audioAiService;
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
    @Path("/audio")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response audio(@Valid AudioRequest req) {
        User user = userRepository.findByPhoneNumber(req.from()).orElseThrow(NotFoundException::new);
        String transcription = audioAiService.transcribe(req.base64());

        List<RecognizedOperation> operations = aiService.handleMessage(transcription).all();

        RecognizedTransaction transaction = operations.stream()
                .filter(op -> AiOperations.ADD_TRANSACTION.equals(op.operation()))
                .map(RecognizedOperation::recognizedTransaction).findFirst().orElse(null);

        if (transaction != null) {
            if (!transaction.withError()) {
                handleTransactions(List.of(transaction), user);
            }
            return Response.ok(transaction).build();
        }

        return Response.status(Response.Status.OK).entity(new RecognizedTransaction(BigDecimal.ZERO,
                "Could not recognize transaction", Transactions.OUT, true, Categories.NONE)).build();
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
        List<RecognizedOperation> response = aiService.handleMessage(message).all();
        return processOnlyAddTransaction(user, response);
    }

    private Response processOnlyAddTransaction(User user, List<RecognizedOperation> messages) {

        List<RecognizedTransaction> onlyAddTransaction = messages.stream()
                .filter(message -> AiOperations.ADD_TRANSACTION.equals(message.operation()))
                .map(recognizedOperation -> recognizedOperation.recognizedTransaction()).toList();

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

    public record MessageRequest(@NotBlank String from, @NotBlank String message) {
    }

    public record AudioRequest(@NotBlank String from, @NotBlank String base64) {
    }

    public record ImageRequest(@NotBlank String from, @NotBlank String base64, String text, @NotBlank String mimeType) {
    }
}
