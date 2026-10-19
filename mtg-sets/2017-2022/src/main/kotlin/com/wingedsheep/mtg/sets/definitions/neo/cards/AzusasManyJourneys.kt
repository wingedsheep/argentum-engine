package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Azusa's Many Journeys // Likeness of the Seeker
 * {1}{G} — Enchantment — Saga // Enchantment Creature — Human Monk 3/3
 *
 * Front — Azusa's Many Journeys:
 *   I — You may play an additional land this turn.
 *   II — You gain 3 life.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Likeness of the Seeker:
 *   Whenever this creature becomes blocked, untap up to three lands you control.
 */
private val LikenessOfTheSeeker = card("Likeness of the Seeker") {
    manaCost = ""
    colorIndicator = "G"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Human Monk"
    oracleText = "Whenever this creature becomes blocked, untap up to three lands you control."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Effects.Pipeline {
            val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
            val toUntap = chooseUpTo(3, from = lands, prompt = "Choose up to three lands to untap")
            run(Effects.TapCollection(collection = toUntap, tap = false))
        }
        description = "Whenever this creature becomes blocked, untap up to three lands you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Lindsey Look"
        flavorText = "\"I ask for nothing more than a new journey and a fresh path to greet my feet when I " +
            "wake each morning.\"\n—Azusa"
        imageUri = "https://cards.scryfall.io/normal/back/e/8/e8a51d2a-1582-4bad-995c-e7fe9f810149.jpg?1783923861"
    }
}

private val AzusasManyJourneysFront = card("Azusa's Many Journeys") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — You may play an additional land this turn.\n" +
        "II — You gain 3 life.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Effects.PlayAdditionalLands(1)
    }

    sagaChapter(2) {
        effect = Effects.GainLife(3)
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Lindsey Look"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8a51d2a-1582-4bad-995c-e7fe9f810149.jpg?1783923861"
    }
}

val AzusasManyJourneys: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = AzusasManyJourneysFront,
    backFace = LikenessOfTheSeeker,
)
