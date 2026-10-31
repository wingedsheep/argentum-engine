package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard

/**
 * Lurrus of the Dream-Den — Ikoria: Lair of Behemoths #226
 * {1}{W/B}{W/B} · Legendary Creature — Cat Nightmare · 3/2
 *
 * Companion — instants and sorceries are free; every permanent card must have mana value 2 or
 * less. The graveyard permission is a once-per-turn, your-turn-only grant for permanent spells
 * (lands are never spells, so no land plays — per its ruling); an {X} spell's announced X counts
 * toward the mana value the permission checks.
 */
val LurrusOfTheDreamDen = card("Lurrus of the Dream-Den") {
    manaCost = "{1}{W/B}{W/B}"
    typeLine = "Legendary Creature — Cat Nightmare"
    power = 3
    toughness = 2
    oracleText = "Companion — Each permanent card in your starting deck has mana value 2 or less. " +
        "(If this card is your chosen companion, you may put it into your hand from outside the game " +
        "for {3} as a sorcery.)\n" +
        "Lifelink\n" +
        "Once during each of your turns, you may cast a permanent spell with mana value 2 or less " +
        "from your graveyard."

    keywordAbility(
        KeywordAbility.companion(
            CompanionCondition.EachCard(
                GameObjectFilter.Nonpermanent or GameObjectFilter.Permanent.manaValueAtMost(2)
            )
        )
    )

    keywords(Keyword.LIFELINK)

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Permanent.manaValueAtMost(2),
            duringYourTurnOnly = true,
            oncePerTurn = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ad36fb2-c44e-4085-ba0d-54277841ad3a.jpg?1783931010"
        ruling("2020-04-17", "Lurrus doesn't care about instant and sorcery cards in your starting deck. They may have any mana value.")
        ruling("2020-04-17", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value. For example, if a permanent spell costs {X}{W}, you could cast it with X as 1 but not as 2.")
        ruling("2020-04-17", "If you cast a spell from your graveyard using another permission, Lurrus's effect doesn't apply. You can cast another permanent spell from your graveyard.")
        ruling("2020-04-17", "If you cast one permanent spell from your graveyard and then have a new Lurrus come under your control in the same turn, you may cast another permanent spell from your graveyard that turn.")
        ruling("2020-04-17", "Lurrus doesn't let you play lands from your graveyard.")
    }
}
