package com.example.reveille

import java.util.Locale

object SpeechParser {
    fun parseSpeechToMissions(speechText: String): List<String> {
        if (speechText.isBlank()) return emptyList()

        var clean = speechText.trim()

        val removePrefixes = listOf(
            "i have ", "my missions are ", "my tasks are ", "today i will ",
            "today i'm going to ", "today i ", "i will ", "i'm going to ",
            "i am going to ", "i plan to ", "i need to ", "here are my missions "
        )
        for (prefix in removePrefixes) {
            val lower = clean.lowercase()
            if (lower.startsWith(prefix)) {
                clean = clean.substring(prefix.length).trim()
                break
            }
        }

        // Strip leading count phrase like "5 missions:" or "4 tasks:"
        clean = clean.replace(Regex("(?i)^(\\d+|one|two|three|four|five|six|seven|eight|nine|ten)\\s+(missions|tasks|points)[:\\s]*"), "").trim()

        // Regex splitting by numbers (1., 2.), ordinals, conjunctions (and, then), or punctuation
        val splitRegex = Regex(
            "(?i)" +
            "\\b(\\d+[.)]|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)\\b|" +
            "\\b(and then|and also|after that|followed by|as well as|then|also|plus|and)\\b|" +
            "[,;.\\n]"
        )

        val rawParts = clean.split(splitRegex)

        val finalTasks = mutableListOf<String>()
        for (part in rawParts) {
            var item = part.trim()
            item = item.replace(Regex("^(i'm|i am|i will|to|next|then|also|and|task|mission|point)\\s+", RegexOption.IGNORE_CASE), "").trim()
            item = item.replace(Regex("^[^a-zA-Z0-9]+"), "").trim()

            if (item.length >= 2 && !item.equals("missions", ignoreCase = true) && !item.equals("tasks", ignoreCase = true)) {
                val capitalized = item.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }
                finalTasks.add(capitalized)
            }
        }

        val distinctTasks = finalTasks.distinct()
        return if (distinctTasks.isNotEmpty()) {
            distinctTasks
        } else {
            listOf(clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() })
        }
    }
}