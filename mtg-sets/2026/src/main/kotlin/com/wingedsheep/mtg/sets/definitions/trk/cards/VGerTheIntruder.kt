package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * V'Ger, the Intruder — Star Trek #87
 * {6}{U} · Legendary Artifact Creature — Construct · 5/7
 *
 * Flying
 * Whenever V'Ger enters or attacks, choose one —
 * • Look at target opponent's hand, then draw a card.
 * • Creatures your opponents control get -1/-0 until your next turn.
 *
 * "Enters or attacks" is two triggered abilities sharing one [ModalEffect.chooseOne] (Appa,
 * Loyal Sky Bison pattern). The -1/-0 mode affects only the creatures opponents control as it
 * resolves — the group is locked in then, so a creature entering later is unaffected.
 */
val VGerTheIntruder = card("V'Ger, the Intruder") {
    manaCost = "{6}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact Creature — Construct"
    power = 5
    toughness = 7
    oracleText = "Flying\n" +
        "Whenever V'Ger enters or attacks, choose one —\n" +
        "• Look at target opponent's hand, then draw a card.\n" +
        "• Creatures your opponents control get -1/-0 until your next turn."

    keywords(Keyword.FLYING)

    val choice = ModalEffect.chooseOne(
        mode("Look at target opponent's hand, then draw a card.") {
            val opponent = target(Targets.Opponent)
            effect = Effects.LookAtHand(opponent) then Effects.DrawCards(1)
        },
        Mode.noTarget(
            Patterns.Group.modifyStatsForAll(
                power = -1,
                toughness = 0,
                filter = GroupFilter.AllCreaturesOpponentsControl,
                duration = Duration.UntilYourNextTurn
            ),
            "Creatures your opponents control get -1/-0 until your next turn."
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = choice
        description = "Whenever V'Ger enters, choose one — look at target opponent's hand, then draw a card; or creatures your opponents control get -1/-0 until your next turn."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = choice
        description = "Whenever V'Ger attacks, choose one — look at target opponent's hand, then draw a card; or creatures your opponents control get -1/-0 until your next turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Jessica Rossier"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b082d454-d331-4960-8120-754f107ffa42.jpg?1785981430"
    }
}
