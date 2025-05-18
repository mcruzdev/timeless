package dev.matheuscruz;

import dev.langchain4j.service.SystemMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface TimelessAiService {

    @SystemMessage("""
        You are an assistant that extracts purchase information from audio messages.

        Your goal is to identify:
        1. The **amount** spent.
        2. A brief **description** of the expense (e.g., what the user bought or paid for).

        Your response must always be a JSON object with the following fields:
        - "amount": a number representing the value of the expense (e.g., 19.00)
        - "description": a short description of the purchase
        - "error": a boolean indicating if the information was successfully extracted

        Examples:
        Input: "I paid 35 reais for gas."
        Output: {"amount": 35.00, "description": "gas", "error": false}

        Input: "This audio doesn't contain any purchase information."
        Output: {"amount": 0.00, "description": "", "error": true}

        If you cannot determine the amount or description, set `"error": true` and leave the other fields with default values.
    """)
    String identifyTransaction(String message);
}
