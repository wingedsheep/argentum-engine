package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Witchbane Orb
 * {4}
 * Artifact
 * When this artifact enters, destroy all Curses attached to you.
 * You have hexproof.
 *
 * The Curses are gathered off the controller's own attachments — [CardSource.AttachedTo] with a
 * player host — and destroyed without targeting. "You have hexproof" is [GrantHexproofToController].
 */
val WitchbaneOrb = card("Witchbane Orb") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, destroy all Curses attached to you.\n" +
        "You have hexproof. (You can't be the target of spells or abilities your opponents control, " +
        "including Aura spells.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val curses = gather(
                CardSource.AttachedTo(
                    host = EffectTarget.Controller,
                    filter = GameObjectFilter.Enchantment.withSubtype(Subtype.CURSE),
                )
            )
            destroy(curses)
        }
        description = "When this artifact enters, destroy all Curses attached to you."
    }

    staticAbility {
        ability = GrantHexproofToController
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53e0bf16-62f5-4b62-96e8-bc7e049bcf89.jpg?1783940895"
    }
}
