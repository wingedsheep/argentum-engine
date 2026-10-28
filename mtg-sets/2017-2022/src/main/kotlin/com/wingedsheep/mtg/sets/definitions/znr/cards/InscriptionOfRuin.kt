package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inscription of Ruin — Zendikar Rising #108
 * {2}{B} · Sorcery
 *
 * Kicker {2}{B}{B}
 * Choose one. If this spell was kicked, choose any number instead.
 * • Target opponent discards two cards.
 * • Return target creature card with mana value 2 or less from your graveyard to the battlefield.
 * • Destroy target creature with mana value 3 or less.
 *
 * Same shape as the other ZNR Inscriptions: the kicked mode count is a cast-time
 * `dynamicChooseCount` over [Conditions.WasKicked] (one mode unkicked, one to three kicked; no mode
 * twice). Targets are all chosen on cast, so the returned creature can't be the destroy target.
 */
val InscriptionOfRuin = card("Inscription of Ruin") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Kicker {2}{B}{B}\n" +
        "Choose one. If this spell was kicked, choose any number instead.\n" +
        "• Target opponent discards two cards.\n" +
        "• Return target creature card with mana value 2 or less from your graveyard to the battlefield.\n" +
        "• Destroy target creature with mana value 3 or less."

    keywordAbility(KeywordAbility.kicker("{2}{B}{B}"))

    spell {
        modal(
            chooseCount = 3,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.WasKicked,
                ifTrue = 3,
                ifFalse = 1
            )
        ) {
            mode("Target opponent discards two cards") {
                val opponent = target(Targets.Opponent)
                effect = Patterns.Hand.discardCards(2, opponent)
            }
            mode("Return target creature card with mana value 2 or less from your graveyard to the battlefield") {
                val creatureCard = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(2))
                effect = Effects.Move(creatureCard, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
            }
            mode("Destroy target creature with mana value 3 or less") {
                val creature = target(TargetFilter.Creature.manaValueAtMost(3))
                effect = Effects.Destroy(creature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93612079-0b8d-489d-9ae1-3593414a8cee.jpg?1783929375"
        ruling(
            "2020-09-25",
            "If more than one mode is chosen, perform them in the order written. Nothing can happen " +
                "in between, however, and no player may choose to take actions. Any abilities that " +
                "trigger will be put onto the stack after the spell has finished resolving."
        )
        ruling("2020-09-25", "If any targets become illegal, the other targets will still be affected as appropriate.")
        ruling(
            "2020-09-25",
            "If a card in a player's graveyard or a creature on the battlefield has {X} in its mana cost, " +
                "X is considered to be 0."
        )
        ruling(
            "2020-09-25",
            "Because targets are chosen as you cast a spell, you can't have Inscription of Ruin return a " +
                "creature card and then destroy that creature."
        )
        ruling("2020-09-25", "If you kick Inscription of Ruin, you can't choose any one mode more than once.")
    }
}
