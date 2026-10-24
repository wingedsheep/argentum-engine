package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silverquill Silencer — Strixhaven: School of Mages #234 (canonical printing)
 * {W}{B} · Creature — Human Cleric · 3/2
 *
 * As this creature enters, choose a nonland card name.
 * Whenever an opponent casts a spell with the chosen name, they lose 3 life and you draw a card.
 *
 * The name is the as-enters [EntersWithChoice] `CARD_NAME` slot over the [CardNamePool.NONLAND]
 * pool (Skyseer's Chariot's shape); the cast trigger reads it through
 * `namedFromChosenComponent()` (Curse of Silence's shape). "They" is the casting opponent,
 * [Player.TriggeringPlayer]. The trigger resolves before the spell that caused it (ruling).
 */
val SilverquillSilencer = card("Silverquill Silencer") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 2
    oracleText = "As this creature enters, choose a nonland card name.\n" +
        "Whenever an opponent casts a spell with the chosen name, they lose 3 life and you draw a card."

    // As this creature enters, choose a nonland card name.
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.NONLAND,
        )
    )

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Any.namedFromChosenComponent())
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.DrawCards(1)
        description = "Whenever an opponent casts a spell with the chosen name, they lose 3 life " +
            "and you draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Zezhou Chen"
        flavorText = "\"I am very much in the mood to start handing out expulsions.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bb60dcc-cb4a-414d-850f-c98efd872894.jpg?1783927292"
        ruling("2021-04-16", "Silverquill Silencer's second triggered ability resolves before the spell that caused it to trigger.")
    }
}
