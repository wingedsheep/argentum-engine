package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Phasing of Zhalfir
 * {2}{U}{U}
 * Enchantment — Saga
 *
 * Read ahead (Choose a chapter and start with that many lore counters. Add one after your draw
 * step. Skipped chapters don't trigger. Sacrifice after III.)
 * I, II — Another target nonland permanent phases out. It can't phase in for as long as you
 *         control this Saga.
 * III — Destroy all creatures. For each creature destroyed this way, its controller creates a
 *       2/2 black Phyrexian creature token.
 *
 * Chapters I and II lock the phase-in rather than linking it to the Saga's departure: once the
 * Saga is sacrificed after III, the permanents phase in at their controllers' next untap steps,
 * not immediately. Chapter III reads each destroyed creature's last-known controller.
 */
val ThePhasingOfZhalfir = card("The Phasing of Zhalfir") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I, II — Another target nonland permanent phases out. It can't phase in for as long as " +
        "you control this Saga.\n" +
        "III — Destroy all creatures. For each creature destroyed this way, its controller " +
        "creates a 2/2 black Phyrexian creature token."

    readAhead()

    sagaChapter(1) {
        val permanent = target(TargetFilter.OtherNonlandPermanent)
        effect = Effects.PhaseOut(permanent, cantPhaseInWhileYouControlSource = true)
    }

    sagaChapter(2) {
        val permanent = target(TargetFilter.OtherNonlandPermanent)
        effect = Effects.PhaseOut(permanent, cantPhaseInWhileYouControlSource = true)
    }

    sagaChapter(3) {
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.Creature, storeDestroyedAs = it)
            }
            run(
                Effects.ForEachInCollection(
                    destroyed,
                    Effects.CreateToken(
                        power = 2,
                        toughness = 2,
                        colors = setOf(Color.BLACK),
                        creatureTypes = setOf("Phyrexian"),
                        controller = EffectTarget.PlayerRef(Player.ControllerOfIterationEntity),
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "L.A. Draws"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0770eb6a-4f01-4677-a401-14c1b30692c9.jpg?1783921347"

        ruling("2022-09-09", "Permanents that phase out as The Phasing of Zhalfir's first and second chapter abilities resolve don't phase in during their controllers' untap steps while the Saga is on the battlefield. After it leaves the battlefield, they will phase in during their controller's next untap step as normal.")
        ruling("2022-09-09", "As a permanent is phased out, Auras and Equipment attached to it also phase out at the same time. Those Auras and Equipment will phase in at the same time that permanent does, and they'll phase in still attached to it.")
        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
