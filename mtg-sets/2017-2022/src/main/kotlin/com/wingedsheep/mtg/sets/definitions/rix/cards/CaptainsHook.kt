package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Captain's Hook — Rivals of Ixalan #177
 * {3} · Artifact — Equipment · Rare
 *
 * Equipped creature gets +2/+0, has menace, and is a Pirate in addition to its other creature types.
 * Whenever this Equipment becomes unattached from a permanent, destroy that permanent.
 * Equip {1}
 *
 * The drawback is Stitcher's Graft's shape with destroy in place of sacrifice:
 * `Triggers.self.becomesUnattached()` fires on every way the Hook can come off (re-equip, the Hook
 * leaving, the CR 704.5n state-based unattach), and "that permanent" is
 * [EffectTarget.AttachedToTriggeringPermanent], the former host carried on the trigger. If the host
 * itself left the battlefield it resolves to nothing, as the ruling says. Unlike sacrifice, destroy
 * works on a former host someone else now controls.
 */
val CaptainsHook = card("Captain's Hook") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0, has menace, and is a Pirate in addition to its other creature types.\n" +
        "Whenever this Equipment becomes unattached from a permanent, destroy that permanent.\n" +
        "Equip {1}"

    staticAbility {
        ability = ModifyStats(+2, +0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantSubtype(Subtype.PIRATE.value, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesUnattached()
        effect = Effects.Destroy(EffectTarget.AttachedToTriggeringPermanent)
        description = "Whenever this Equipment becomes unattached from a permanent, destroy that permanent."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Sara Winters"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bd3f777-b33a-454a-8d5d-993260ddda03.jpg?1783935265"

        ruling("2018-01-19", "Captain's Hook becomes unattached from the creature it's equipping if you equip it to a new creature, if Captain's Hook leaves the battlefield, if the equipped creature ceases to be a creature, or if Captain's Hook ceases to be an Equipment. (It also becomes unattached if the equipped creature leaves the battlefield, but the triggered ability won't do anything in that case.)")
    }
}
