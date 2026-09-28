package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crush Underfoot
 * {1}{R}
 * Kindred Instant — Giant
 * Choose a Giant creature you control. It deals damage equal to its power to target creature.
 *
 * Two decisions at two different times, and the card is only right if they stay apart:
 *
 *  - **"target creature"** is a printed target, declared when Crush Underfoot goes on the stack
 *    (index 0), so shroud/hexproof/protection are checked at announcement and the spell fizzles
 *    if the creature is gone on resolution.
 *  - **"Choose a Giant creature you control"** is *not* a target. It is picked mid-resolution by
 *    [SelectTargetEffect], after priority has passed — so an opponent can't respond to which
 *    Giant you picked, and the choice is made against the board as it stands when the spell
 *    resolves rather than as it stood when you cast it. Because it's a choice and not a target,
 *    it's `nonTargeting`: a Giant of yours with shroud can still be the one that swings.
 *
 * That order — target first, Giant second — is the rules' order, so the resolution prompt
 * carries the context: it says what the Giant is for and that the creature was already
 * targeted, rather than reading like a second target.
 *
 * The chosen Giant lands in the resolution pipeline's `crushGiant` collection, which the damage
 * step then reads twice: [EffectTarget.PipelineTarget] for "equal to its power" (the generic
 * `storedCollections` reader — the linter pairs it with `SelectTarget.storeAs`) and
 * [EffectTarget.PipelineTarget] as the `damageSource`, so the damage is dealt *by the Giant*.
 * That distinction is load-bearing: it makes the damage red-creature damage rather than spell
 * damage, so lifelink, deathtouch, and "prevent all damage a creature would deal" all read off
 * the Giant.
 *
 * Both zero-Giant and zero-power cases fizzle gracefully. With no Giant to choose, the select
 * step stores an empty collection and [DealDamageEffect] skips the whole instruction rather than
 * falling back to Crush Underfoot itself as the source (CR 608.2b); a 0-power Giant deals no
 * damage because the amount is not positive.
 *
 * "Kindred Instant — Giant" is the 2024 errata of the printed "Tribal Instant — Giant": the card
 * has the Giant creature type in every zone, so it is itself fetched by Lorwyn's Giant-matters
 * cards (Ancient Amphitheater, Boldwyr Intimidator).
 */
val CrushUnderfoot = card("Crush Underfoot") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Kindred Instant — Giant"
    oracleText = "Choose a Giant creature you control. It deals damage equal to its power to target creature."

    spell {
        val victim = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val crushGiant = selectTarget(
                TargetObject(filter = TargetFilter.Creature.youControl().withSubtype(Subtype.GIANT)),
                nonTargeting = true,
                prompt = "Choose a Giant you control — it deals damage equal to its power to the targeted creature"
            )
            run(Effects.DealDamage(
                amount = DynamicAmounts.powerOf(crushGiant.asTarget),
                target = victim,
                damageSource = crushGiant.asTarget
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Steven Belledin"
        flavorText = "Five-toed grave\n—Kithkin phrase meaning \"a giant's footprint\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0386925-53bc-4902-ac30-cbdcb099936d.jpg?1783942877"
    }
}
