package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cindervines
 * {R}{G}
 * Enchantment
 * Whenever an opponent casts a noncreature spell, this enchantment deals 1 damage to that player.
 * {1}, Sacrifice this enchantment: Destroy target artifact or enchantment. This enchantment deals
 * 2 damage to that permanent's controller.
 *
 * "That player" is the caster, bound by the opponent-casts trigger as [Player.TriggeringPlayer]
 * (Kambal, Consul of Allocation's binding). The activated ability deals its 2 damage to the
 * target's controller ([EffectTarget.TargetController]) before the destroy, so the controller is
 * read while the permanent is still on the battlefield (the Detonate / Crumble shape). Per the
 * rulings, a legal target that survives the destroy (indestructible) still has its controller
 * dealt damage, and an illegal target fizzles the whole ability.
 */
val Cindervines = card("Cindervines") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent casts a noncreature spell, this enchantment deals 1 damage to that player.\n" +
        "{1}, Sacrifice this enchantment: Destroy target artifact or enchantment. " +
        "This enchantment deals 2 damage to that permanent's controller."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts(GameObjectFilter.Noncreature)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever an opponent casts a noncreature spell, Cindervines deals 1 damage to that player."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeSelf
        )
        val permanent = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.DealDamage(2, EffectTarget.TargetController) then
            Effects.Destroy(permanent)
        description = "Destroy target artifact or enchantment. Cindervines deals 2 damage to that permanent's controller."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f970f79-3051-4ba1-badb-697ef321cbb3.jpg?1783933655"
        ruling("2019-01-25", "Cindervines's triggered ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2019-01-25", "If the target permanent is an illegal target by the time Cindervines's activated ability tries to resolve, the ability doesn't resolve. No player is dealt damage. If the target is legal but not destroyed (most likely because it has indestructible), its controller is dealt damage.")
        ruling("2019-01-25", "Cindervines can be the target of its own activated ability. The ability won't resolve since it won't have a legal target, and no player will be dealt damage, but this does allow you to sacrifice Cindervines without anything else to target if you want to.")
    }
}
