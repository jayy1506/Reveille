package com.example.reveille

import java.time.LocalDate

object DailyQuotes {
    private val quotes = listOf(
        "Discipline is choosing between what you want now and what you want most.",
        "Suffer the pain of discipline, or suffer the pain of regret.",
        "Excuses are the nails used to build a house of failure. Execute your plan!",
        "Small daily victories lead to massive lifetime achievements. Get 1% better today.",
        "When you feel like quitting, remember why you started, soldier!",
        "Success isn't owned. It's leased, and rent is due every single day.",
        "Action destroys fear. Move out and tackle your toughest mission first!",
        "Don't count the days, make the days count. Zero excuses!"
    )

    fun todayQuote(): String {
        val dayOfYear = LocalDate.now().dayOfYear
        return quotes[dayOfYear % quotes.size]
    }
}