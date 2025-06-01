package dev.matheuscruz.infra.ai;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.service.UserMessage;
import dev.matheuscruz.infra.ai.data.AiImageResponse;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(modelName = "gpt-4-turbo")
public interface TimelessImageAiService {

    @UserMessage("""
            You are a financial assistant specialized in extracting transaction data from Base64-encoded images. These images may contain receipts, invoices, letters, or handwritten notes.
            
            Your task:
            1. Decode the Base64 image.
            2. Identify and extract the most relevant transaction information from the content.
            3. Return a JSON object with the following structure:
            
            {
              "amount": number,             // Total transaction value (e.g., 99.75)
              "description": string,        // Brief description of the transaction (e.g., product or service)
              "type": "IN" | "OUT",         // "IN" if money was received, "OUT" if money was spent
              "category": string,           // Financial category in English (one of: GOALS, COMFORT, FIXED_COSTS, PLEASURES, FINANCIAL_FREEDOM, KNOWLEDGE)
              "withError": boolean          // true if the data is missing or ambiguous
            }
            
            Rules:
            - Use the actual amount paid as the transaction value, typically found in fields like "Total", "Card", "Total R$", "Amount Due", "Final Amount", etc.
            - Ignore taxes, discounts, change, or per-item prices.
            - If a credit card payment field is present, prefer that as the transaction amount.
            - "type" should be:
              - "OUT" for purchases, payments, or expenses.
              - "IN" for received money, refunds, or deposits.
            - "category" should be one of the following, based on the transaction:
              - "GOALS"
              - "COMFORT"
              - "FIXED_COSTS"
              - "PLEASURES"
              - "FINANCIAL_FREEDOM"
              - "KNOWLEDGE"
            - If the content contains anything between triple dashes (`---`), **always** use that as the final `description`, regardless of other content.
            
            If any value cannot be confidently extracted:
            - amount: 0.00
            - description: ""
            - type: "OUT"
            - category: "NONE"
            - withError: true
            
            Important:
            - Do not include any explanations or formatting.
            - Only return the **raw JSON object**.
            
            ---
            
            Final description override:
            If a block of text exists between three dashes (---), use that content as the final `description`:
            ---
            {description}
            ---
            """)
    AiImageResponse handleTransactionImage(Image image, String description);
}
