package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Long Reach of Night // Animus of Night's Reach
 * {3}{B} — Enchantment — Saga // Enchantment Creature — Spirit 0/4
 *
 * Front — The Long Reach of Night:
 *   I, II — Each opponent sacrifices a creature of their choice unless they discard a card.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Animus of Night's Reach:
 *   Menace
 *   Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of
 *   creature cards in defending player's graveyard.
 *
 * Each opponent decides in turn whether to discard: [Effects.ForEachPlayer] rebinds the controller
 * to the opponent being processed, so both the payer and the sacrificer are that opponent. X is
 * counted once, as the attack trigger resolves.
 */
private val AnimusOfNightsReach = card("Animus of Night's Reach") {
    manaCost = ""
    colorIndicator = "B"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Spirit"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of " +
        "creature cards in defending player's graveyard."
    power = 0
    toughness = 4

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(
            power = DynamicAmounts.zone(Player.DefendingPlayer, Zone.GRAVEYARD, GameObjectFilter.Creature).count(),
            toughness = DynamicAmounts.fixed(0),
            target = EffectTarget.Self,
        )
        description = "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the " +
            "number of creature cards in defending player's graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Tuan Duong Chu"
        flavorText = "The dark myojin's gifts were instrumental in ending the Kami War."
        imageUri = "https://cards.scryfall.io/normal/back/4/e/4ec18bf7-bd80-408d-acc4-ffaaf7ef6b5b.jpg?1783923888"
    }
}

private val TheLongReachOfNightFront = card("The Long Reach of Night") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Each opponent sacrifices a creature of their choice unless they discard a card.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            effect = Effects.ForEachPlayer(
                Player.EachOpponent,
                listOf(
                    Effects.PayOrSuffer(
                        cost = Costs.pay.Discard(count = 1),
                        suffer = Effects.Sacrifice(GameObjectFilter.Creature, target = EffectTarget.Controller),
                        player = EffectTarget.Controller,
                    )
                )
            )
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ec18bf7-bd80-408d-acc4-ffaaf7ef6b5b.jpg?1783923888"
    }
}

val TheLongReachOfNight: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheLongReachOfNightFront,
    backFace = AnimusOfNightsReach,
)
