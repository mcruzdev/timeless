package dev.matheuscruz.infra.ai;

import dev.langchain4j.service.UserMessage;
import dev.matheuscruz.domain.notification.NotificationService;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(tools = { NotificationService.class })
public interface TimelessAiService {

    @UserMessage("""
            You are an assistant that extracts financial transaction data from user messages.

            Your task is to analyze the content between the --- delimiters and return a JSON object with the following fields:

            {
              "amount": number,         // The monetary value involved, e.g., 19.00
              "description": string,    // A short description of the transaction (e.g., What was Bought/Sold/Received)
              "type": "IN" | "OUT",     // "IN" if the user received money, "OUT" if the user paid or spent money
              "error": boolean          // true if any required information is missing or ambiguous
            }

            Instructions:
            - If the user received money, set "type" to "IN".
            - If the user spent or paid money, set "type" to "OUT".
            - If you cannot confidently determine any of the fields, set "error" to true and use default values for the others:
              - amount: 0.00
              - description: ""
              - type: "OUT"

            Examples:

            Input:
            "I paid 35 reais for gas at shopping mall station."
            Output:
            {
              "amount": 35.00,
              "description": "Gas at shopping mall station",
              "type": "OUT",
              "error": false
            }

            Input:
            "I received 500 from a freelance job."
            Output:
            {
              "amount": 500.00,
              "description": "Freelance job",
              "type": "IN",
              "error": false
            }

            Input:
            "This audio doesn't contain any purchase information."
            Output:
            {
              "amount": 0.00,
              "description": "",
              "type": "OUT",
              "error": true
            }

            Input (user just wants to send the result by email):
            - The output must still follow the same JSON format described above.

            ---
            {message}
            ---
            """)
    String identifyTransaction(String message);
}
