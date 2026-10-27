package com.wingedsheep.mtg.sets.definitions.`40k`.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Celestine, the Living Saint — Warhammer 40,000 Commander #10
 * {4}{W} · Legendary Creature — Human Warrior · 3/4
 *
 * Flying, lifelink
 * Healing Tears — At the beginning of your end step, return target creature card with mana value X
 * or less from your graveyard to the battlefield, where X is the amount of life you gained this turn.
 *
 * The Moseo shape without its intervening-if or "up to one": a mandatory target capped by
 * `manaValueAtMostDynamic(lifeGainedThisTurn)` — total life gained, never netted against life lost
 * (ruling) — checked as the trigger is put on the stack and again on resolution.
 */
val CelestineTheLivingSaint = card("Celestine, the Living Saint") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 4
    oracleText = "Flying, lifelink\n" +
        "Healing Tears — At the beginning of your end step, return target creature card with mana value X " +
        "or less from your graveyard to the battlefield, where X is the amount of life you gained this turn."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.ownedByYou()
                    .manaValueAtMostDynamic(DynamicAmounts.lifeGainedThisTurn(Player.You)),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.PutOntoBattlefield(t)
        description = "Healing Tears — At the beginning of your end step, return target creature card with " +
            "mana value X or less from your graveyard to the battlefield, where X is the amount of life you " +
            "gained this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a099cbd2-e92a-433a-bc51-0aa2f8fc6857.jpg?1783920947"
        ruling(
            "2022-10-07",
            "Celestine's triggered ability looks at the total amount of life you gained to determine X, regardless " +
                "of whether or not you also lost any life. For example, if you gained 3 life this turn, X is 3, even " +
                "if you also lost some amount of life in the same turn."
        )
        ruling(
            "2022-10-07",
            "The target creature card is chosen as the triggered ability is put on the stack. Gaining life after " +
                "that point won't let you choose a target with a higher mana value."
        )
    }
}
