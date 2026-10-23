package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glowcap Lantern (LCI #187) — {G} Artifact — Equipment (uncommon)
 *
 * Equipped creature has "You may look at the top card of your library any time" and
 * "Whenever this creature attacks, it explores."
 * Equip {2}
 *
 * Implementation notes:
 * - "Whenever this creature attacks, it explores" is granted to the equipped creature via
 *   [GrantTriggeredAbility] + `Triggers.<subject>.attacks(requires)` (SELF binding). The [Effects.Explore] targets
 *   [EffectTarget.Self] — the granted ability's source, i.e. the equipped creature — so "it"
 *   explores and any +1/+1 counter lands on that creature.
 * - "You may look at the top card of your library any time" is the filterless player-permission
 *   static [LookAtTopOfLibrary], which grants its controller the private peek. It lives on the
 *   Equipment (there is no primitive to scope a filterless player static onto the equipped
 *   creature) but is gated on the Equipment being attached — a conditional static whose
 *   condition is [Conditions.SourceMatches] "attached to a creature" — so an *unattached*
 *   Glowcap Lantern grants no peek, matching the printed card (the permission only exists while
 *   attached, as part of "Equipped creature has ...").
 */
val GlowcapLantern = card("Glowcap Lantern") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"You may look at the top card of your library any time\" and " +
        "\"Whenever this creature attacks, it explores.\" (Reveal the top card of your library. Put that " +
        "card into your hand if it's a land. Otherwise, put a +1/+1 counter on that creature, then put " +
        "the card back or put it into your graveyard.)\n" +
        "Equip {2}"

    // "You may look at the top card of your library any time" — only while attached (see KDoc).
    staticAbility {
        condition = Conditions.SourceMatches(GameObjectFilter.Any.attachedTo(GameObjectFilter.Creature))
        ability = LookAtTopOfLibrary()
    }

    // Equipped creature has "Whenever this creature attacks, it explores."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.attacks(),
                effect = Effects.Explore(EffectTarget.Self)
            ),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bafde87c-743d-4307-93e0-fbd30f5d92f6.jpg?1782694459"
    }
}
