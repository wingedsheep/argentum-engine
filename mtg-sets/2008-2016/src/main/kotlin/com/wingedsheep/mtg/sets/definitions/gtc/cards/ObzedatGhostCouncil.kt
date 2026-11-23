package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Obzedat, Ghost Council
 * {1}{W}{W}{B}{B}
 * Legendary Creature — Spirit Advisor
 * 5/5
 * When Obzedat enters, target opponent loses 2 life and you gain 2 life.
 * At the beginning of your end step, you may exile Obzedat. If you do, return it to the battlefield
 * under its owner's control at the beginning of your next upkeep. It gains haste.
 *
 * The end-step trigger is a "you may" gated on Obzedat still being on the battlefield
 * (`sourceRequiredZone`). The exile is a tracked move, and the upkeep delayed trigger carries that
 * pile (`carryCollections`), so it returns only if it actually reached exile and is still there.
 * `fireOnPlayer = You` keeps the return on the upkeep of whoever controlled the end-step ability
 * (ruling: a "until end of turn" thief brings it back on *their* next upkeep, under its owner's
 * control). The returned Obzedat gains haste permanently — it keeps it for as long as it stays on
 * the battlefield, even under another controller.
 */
val ObzedatGhostCouncil = card("Obzedat, Ghost Council") {
    manaCost = "{1}{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Spirit Advisor"
    power = 5
    toughness = 5
    oracleText = "When Obzedat enters, target opponent loses 2 life and you gain 2 life.\n" +
        "At the beginning of your end step, you may exile Obzedat. If you do, return it to the " +
        "battlefield under its owner's control at the beginning of your next upkeep. It gains haste."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.May(
            effect = Effects.Pipeline {
                val self = gather(CardSource.Entity(EffectTarget.Self))
                val exiled = moveTracked(self, CardDestination.ToZone(Zone.EXILE), name = "obzedatExiled")
                run(Effects.CreateDelayedTrigger(
                    step = Step.UPKEEP,
                    fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                    effect = Effects.Pipeline {
                        val returned = moveTracked(
                            exiled,
                            CardDestination.ToZone(Zone.BATTLEFIELD),
                            underOwnersControl = true,
                            name = "obzedatReturned"
                        )
                        ifNotEmpty(returned) {
                            run(Effects.GrantKeyword(Keyword.HASTE, returned.asTarget, Duration.Permanent))
                        }
                    },
                    carryCollections = listOf(exiled.key)
                ))
            },
            sourceRequiredZone = Zone.BATTLEFIELD,
            descriptionOverride = "You may exile Obzedat. If you do, return it to the battlefield under " +
                "its owner's control at the beginning of your next upkeep. It gains haste."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "182"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cc198d8-1f27-482d-8f5d-21e02c59797a.jpg?1783940104"

        ruling(
            "2017-03-14",
            "Once Obzedat's ability has returned it to the battlefield, it has haste for as long as it remains on the battlefield. If an opponent gains control of it (perhaps with Olivia Voldaren or Soul Ransom), it can attack that turn."
        )
        ruling(
            "2017-03-14",
            "If you gain control of Obzedat \"until end of turn,\" you'll control it during your end step and you choose whether to exile it. If you do, it returns to the battlefield under its owner's control at the beginning of your next upkeep, not its owner's next upkeep."
        )
    }
}
