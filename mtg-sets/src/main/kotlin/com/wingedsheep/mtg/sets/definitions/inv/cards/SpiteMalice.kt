package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Spite // Malice (INV 293) — split-layout instant.
 *
 * Spite {3}{U} — Instant
 *   Counter target noncreature spell.
 *
 * Malice {3}{B} — Instant
 *   Destroy target nonblack creature. It can't be regenerated.
 *
 * Cast either half separately.
 */
val SpiteMalice = card("Spite // Malice") {
    layout = CardLayout.SPLIT
    colorIdentity = "UB"

    face("Spite") {
        manaCost = "{3}{U}"
        typeLine = "Instant"
        oracleText = "Counter target noncreature spell."

        spell {
            target = Targets.NoncreatureSpell
            effect = Effects.CounterSpell()
        }
    }

    face("Malice") {
        manaCost = "{3}{B}"
        typeLine = "Instant"
        oracleText = "Destroy target nonblack creature. It can't be regenerated."

        spell {
            val creature = target(
                "target nonblack creature",
                TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK))
            )
            effect = CantBeRegeneratedEffect(creature) then Effects.Destroy(creature)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "293"
        artist = "David Martin"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/054f1845-196f-41c1-9682-042171cccd49.jpg?1562896042"
    }
}
