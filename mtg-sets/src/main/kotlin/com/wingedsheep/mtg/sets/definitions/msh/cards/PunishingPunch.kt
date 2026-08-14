package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Punishing Punch (MSH #180) — {2}{G} Instant
 *
 * This spell costs {2} less to cast if there are two or more creature cards in your graveyard.
 * Target creature you control deals damage equal to twice its power to target creature an
 * opponent controls.
 *
 * The reduction is the Truck Toss shape: a self-cast [ModifySpellCost] whose whole modification is
 * gated on a state condition ([CostGating.OnlyIf] over
 * [Conditions.CreatureCardsInGraveyardAtLeast]) rather than folded into the amount. Only the
 * generic {2} is reduced; the {G} pip is untouched.
 *
 * The damage clause is the Polliwallop shape — two independent targets, the damage attributed to
 * the *first* one via `damageSource` (so deathtouch, lifelink, and "dealt damage by" triggers all
 * read the punching creature), with the amount recomputed on resolution as twice that creature's
 * current power ([DynamicAmount.Multiply] over its [EntityNumericProperty.Power]). If either
 * target is illegal on resolution the spell doesn't resolve / deals no damage.
 */
val PunishingPunch = card("Punishing Punch") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if there are two or more creature cards in " +
        "your graveyard.\n" +
        "Target creature you control deals damage equal to twice its power to target creature an " +
        "opponent controls."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.CreatureCardsInGraveyardAtLeast(2)),
        )
    }

    spell {
        val myCreature = target("target creature you control", Targets.CreatureYouControl)
        val theirCreature = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = DealDamageEffect(
            amount = DynamicAmount.Multiply(
                DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
                2,
            ),
            target = theirCreature,
            damageSource = myCreature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Bachzim"
        flavorText = "\"I can't help it! Your face is just so punchable!\"\n—Titania, Mary MacPherran"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a33a4cb4-1b57-47ca-8e5e-58ff46a6e0ce.jpg?1783902913"
    }
}
