package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mask of Immolation
 * {1}{R}
 * Artifact — Equipment
 * When this Equipment enters, create a 1/1 red Elemental creature token, then attach this
 *   Equipment to it.
 * Equipped creature has "Sacrifice this creature: It deals 1 damage to any target."
 * Equip {2}
 *
 * The ETB is the same create-then-attach chain as the Job select / For Mirrodin! shell: the token
 * publishes its id to [CREATED_TOKENS] and the Equipment attaches to it. The granted ability's
 * source is the equipped creature (it is granted to and activated from that creature), so the
 * damage is dealt by that creature — not by the Mask — as the second ruling requires.
 */
val MaskOfImmolation = card("Mask of Immolation") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a 1/1 red Elemental creature token, then attach this Equipment to it.\n" +
        "Equipped creature has \"Sacrifice this creature: It deals 1 damage to any target.\"\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            imageUri = "https://cards.scryfall.io/normal/front/0/5/0574973b-a0d1-4e17-9ed7-42a98d25bb8d.jpg?1783932855"
        ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.SacrificeSelf
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(1, anyTarget)
            },
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be2f4165-f87f-454b-b955-e4477c864e95.jpg?1783932975"
        ruling(
            "2019-07-12",
            "No player may take any actions between the time you create the Elemental token and the time Mask of Immolation becomes attached to it."
        )
        ruling(
            "2019-07-12",
            "The equipped creature is the source of the activated ability and the source of the damage, not Mask of Immolation. This means that a green creature equipped by Mask of Immolation may deal damage to a creature with protection from red but may not deal damage to a creature with protection from green."
        )
    }
}
