package io.github.behnooddev.voidmanager.core.model

import io.github.behnooddev.voidmanager.core.model.FieldDataType as T
import io.github.behnooddev.voidmanager.core.model.FieldSection as S
import io.github.behnooddev.voidmanager.core.model.Sensitivity as L

/** The built-in field definitions. Changing this list changes [VERSION]. */
object FieldRegistry {
    const val VERSION = 1

    private fun part(
        key: String,
        level: L,
    ) = FieldPartDefinition(key, level)

    val definitions: List<FieldDefinition> =
        listOf(
            FieldDefinition("name", S.Identity, T.Text, "Name", L.Public, allowsMultiple = false),
            FieldDefinition("age", S.Identity, T.Number, "Age", L.Personal, allowsMultiple = false),
            FieldDefinition("national_id", S.Identity, T.Text, "National ID", L.Sensitive, allowsMultiple = false),
            FieldDefinition("phone", S.Contact, T.Phone, "Phone", L.Personal, allowsMultiple = true),
            FieldDefinition("email", S.Contact, T.Email, "Email", L.Personal, allowsMultiple = true),
            FieldDefinition("telegram", S.Social, T.Username, "Telegram", L.Personal, allowsMultiple = true),
            FieldDefinition("instagram", S.Social, T.Username, "Instagram", L.Personal, allowsMultiple = true),
            FieldDefinition("whatsapp", S.Social, T.Phone, "WhatsApp", L.Personal, allowsMultiple = true),
            FieldDefinition("social_other", S.Social, T.Username, "Other social account", L.Personal, true),
            FieldDefinition(
                "account",
                S.Accounts,
                T.Account,
                "Account",
                L.Personal,
                allowsMultiple = true,
                parts =
                    listOf(
                        part("service", L.Personal),
                        part("website", L.Personal),
                        part("username", L.Personal),
                        part("password", L.Secret),
                        part("recovery", L.Secret),
                    ),
            ),
            FieldDefinition(
                "card",
                S.Financial,
                T.Card,
                "Bank card",
                L.Sensitive,
                allowsMultiple = true,
                parts =
                    listOf(
                        part("holder", L.Sensitive),
                        part("bank", L.Sensitive),
                        part("number", L.Sensitive),
                        part("expiry", L.Sensitive),
                        part("iban", L.Sensitive),
                        part("cvv2", L.Secret),
                        part("pin", L.Secret),
                    ),
            ),
            FieldDefinition("bank_info", S.Financial, T.Multiline, "Bank information", L.Sensitive, true),
            FieldDefinition(
                "address",
                S.Address,
                T.Address,
                "Address",
                L.Personal,
                allowsMultiple = true,
                parts =
                    listOf(
                        part("street", L.Personal),
                        part("city", L.Personal),
                        part("region", L.Personal),
                        part("postal_code", L.Personal),
                        part("country", L.Personal),
                    ),
            ),
            FieldDefinition(
                "work",
                S.Work,
                T.Work,
                "Work",
                L.Personal,
                allowsMultiple = true,
                parts =
                    listOf(
                        part("company", L.Personal),
                        part("role", L.Personal),
                        part("phone", L.Personal),
                        part("email", L.Personal),
                        part("website", L.Personal),
                    ),
            ),
            FieldDefinition(
                "education",
                S.Education,
                T.Education,
                "Education",
                L.Personal,
                allowsMultiple = true,
                parts =
                    listOf(
                        part("institution", L.Personal),
                        part("field", L.Personal),
                        part("degree", L.Personal),
                        part("start_date", L.Personal),
                        part("end_date", L.Personal),
                    ),
            ),
            FieldDefinition("birthday", S.Dates, T.Date, "Birthday", L.Personal, allowsMultiple = false),
            FieldDefinition("anniversary", S.Dates, T.Date, "Anniversary", L.Personal, allowsMultiple = true),
            FieldDefinition("important_date", S.Dates, T.Date, "Important date", L.Personal, allowsMultiple = true),
            FieldDefinition("note", S.Notes, T.Multiline, "Note", L.Personal, allowsMultiple = true),
        )

    private val byKey: Map<String, FieldDefinition> = definitions.associateBy { it.systemKey }

    fun bySystemKey(systemKey: String): FieldDefinition? = byKey[systemKey]
}
