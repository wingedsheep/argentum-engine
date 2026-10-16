package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Citizen's Crowbar
 * {1}{W}
 * Artifact — Equipment
 * When this Equipment enters, create a 1/1 green and white Citizen creature token, then attach
 * this Equipment to it.
 * Equipped creature gets +1/+1 and has "{W}, {T}, Sacrifice Citizen's Crowbar: Destroy target
 * artifact or enchantment."
 * Equip {2}
 *
 * The enter trigger is the Elven Bow shell: the token is published to [CREATED_TOKENS] and
 * [Effects.AttachEquipment] reads it back. The quoted ability is the Blazing Torch idiom — granted
 * to the bearer, whose {T} it taps, with "Sacrifice Citizen's Crowbar" as
 * [Costs.SacrificeGrantingPermanent] (the specific Equipment, never the creature). A player who
 * steals the creature but not the Crowbar can't pay that cost, per the ruling.
 */
val CitizensCrowbar = card("Citizen's Crowbar") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, create a 1/1 green and white Citizen creature token, then attach this Equipment to it.\n" +
        "Equipped creature gets +1/+1 and has \"{W}, {T}, Sacrifice Citizen's Crowbar: Destroy target artifact or enchantment.\"\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Citizen"),
        ) then Effects.AttachEquipment(EffectTarget.PipelineTarget(CREATED_TOKENS, 0))
        description = "When this Equipment enters, create a 1/1 green and white Citizen creature token, then attach this Equipment to it."
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(
                    Costs.Mana("{W}"),
                    Costs.Tap,
                    Costs.SacrificeGrantingPermanent
                )
                val permanent = target(TargetFilter.ArtifactOrEnchantment)
                effect = Effects.Destroy(permanent)
                description = "{W}, {T}, Sacrifice Citizen's Crowbar: Destroy target artifact or enchantment."
            },
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b68896f5-1e13-400a-a327-b6c6f00858b7.jpg?1783923162"
        ruling(
            "2022-04-29",
            "If another player gains control of the equipped creature but doesn't gain control of the Equipment, " +
                "they will not be able to activate the ability the Equipment grants because they can't pay a cost " +
                "that includes sacrificing a permanent they don't control."
        )
    }
}
