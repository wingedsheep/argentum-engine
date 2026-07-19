package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hawkeye's Bow (MSH #132) — {R} Artifact — Equipment
 *
 * Equipped creature gets +1/+0 and has reach.
 * Whenever equipped creature becomes tapped, it deals 1 damage to each opponent.
 * Equip {1}
 *
 * Implementation notes:
 * - The pump and the reach grant are two statics scoped to [Filters.EquippedCreature]
 *   (the Captain America's Shield idiom).
 * - "Whenever equipped creature becomes tapped" is the tap trigger bound to the attached
 *   creature ([TriggerBinding.ATTACHED]) — `AttachmentTriggerDetector` matches a `TappedEvent`
 *   on the equipped permanent, so it fires for *any* tap (attacking, crew, an opponent's
 *   tapper), not only for taps the controller causes.
 * - "**it** deals 1 damage" — the damage source is the equipped creature, not the Equipment,
 *   so `damageSource = EffectTarget.EquippedCreature` (matters for lifelink/deathtouch and for
 *   damage-source predicates); the recipient is [Player.EachOpponent], not a target.
 */
val HawkeyesBow = card("Hawkeye's Bow") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+0 and has reach.\n" +
        "Whenever equipped creature becomes tapped, it deals 1 damage to each opponent.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(1, 0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.becomesTapped(binding = TriggerBinding.ATTACHED)
        effect = Effects.DealDamage(
            amount = 1,
            target = EffectTarget.PlayerRef(Player.EachOpponent),
            damageSource = EffectTarget.EquippedCreature,
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Marc Aspinall"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/485be72b-d784-4886-bb3a-48cff8f781c6.jpg?1783902931"
    }
}
