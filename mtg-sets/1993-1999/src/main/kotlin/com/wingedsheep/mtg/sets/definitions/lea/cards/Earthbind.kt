package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Earthbind
 * {R}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, if enchanted creature has flying, this Aura deals 2 damage to that
 * creature and this Aura gains "Enchanted creature loses flying."
 *
 * "If enchanted creature has flying" is an intervening-if: checked from projected keywords when
 * the trigger would fire and again on resolution, so a creature that lost flying in between takes
 * no damage and the Aura gains nothing.
 *
 * The *Aura* gains the static, not the creature: it is appended to Earthbind's own continuous-
 * effect sources via `BecomeArtifact` with every transform knob off (`cardTypes`/`subtypes`/
 * `colors = null`, `loseAllAbilities = false`), the same channel a printed static uses, so it
 * projects in Layer 6 onto whatever Earthbind is attached to and ends when Earthbind leaves the
 * battlefield. `Effects.GrantStaticAbility` would be inert here — the layer projector never reads
 * its point-of-use store.
 */
val Earthbind = card("Earthbind") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, if enchanted creature has flying, this Aura deals 2 damage to " +
        "that creature and this Aura gains \"Enchanted creature loses flying.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.EnchantedPermanentMatches(
            GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
        )
        effect = Effects.DealDamage(2, EffectTarget.EnchantedCreature) then
            Effects.BecomeArtifact(
                target = EffectTarget.Self,
                cardTypes = null,
                subtypes = null,
                colors = null,
                loseAllAbilities = false,
                grantedStaticAbilities = listOf(RemoveKeywordStatic(Keyword.FLYING)),
                duration = Duration.Permanent,
            )
        description = "When this Aura enters, if enchanted creature has flying, this Aura deals " +
            "2 damage to that creature and this Aura gains \"Enchanted creature loses flying.\""
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6d492b7-b0b3-420e-8d00-6dacb11de77e.jpg?1783948687"

        ruling(
            "2004-10-04",
            "If the enchanted creature gains flying after Earthbind is put onto it, it will have " +
                "flying. The two effects are simply applied in timestamp order.",
        )
    }
}
