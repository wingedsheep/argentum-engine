package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Smoldering Werewolf // Erupting Dreadwolf (Eldritch Moon #142)
 * {2}{R}{R}
 * Creature — Werewolf Horror 3/2 // Creature — Eldrazi Werewolf 6/4
 *
 * Front — "When this creature enters, it deals 1 damage to each of up to two target creatures."
 *         "{4}{R}{R}: Transform this creature."
 * Back  — "Whenever this creature attacks, it deals 2 damage to any target."
 *
 * "Each of up to two target creatures" is **one** target requirement taking up to two objects
 * (`TargetCreature(count = 2, optional = true)` → `minCount = 0`), not two independent slots — so
 * the two picks must be different creatures (CR 601.2c) and choosing zero is legal. The damage is
 * then dealt once per chosen creature via [ForEachTargetEffect]; leaving `damageSource` null
 * attributes it to the trigger's source, matching "**it** deals 1 damage" (the Rollercrusher Ride
 * idiom).
 *
 * The back face is a colorless Eldrazi Werewolf (no mana cost and no color indicator on the printed
 * card), so it carries no `colorIndicator`; `colorIdentity` stays "R" because identity is a property
 * of the whole card, not of one face.
 *
 * This is not a Daybound/Nightbound werewolf — the flip is an ordinary activated
 * [TransformEffect], one-way (the back has no way back).
 */

private val SmolderingWerewolfFront = card("Smoldering Werewolf") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Werewolf Horror"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, it deals 1 damage to each of up to two target creatures.\n" +
        "{4}{R}{R}: Transform this creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("up to two target creatures", TargetCreature(count = 2, optional = true))
        effect = ForEachTargetEffect(
            listOf(Effects.DealDamage(1, EffectTarget.ContextTarget(0)))
        )
        description = "When this creature enters, it deals 1 damage to each of up to two target creatures."
    }

    activatedAbility {
        cost = Costs.Mana("{4}{R}{R}")
        effect = TransformEffect(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Zack Stella"
        flavorText = "\"Never thought I'd see the day I'd be wishing to see just a plain old werewolf.\"\n" +
            "—Raf Gyel of the Quiver of Kessig"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b0eab47-af62-4ee8-99cf-a864fadade2d.jpg?1783937460"
        ruling(
            "2016-07-13",
            "Erupting Dreadwolf's triggered ability resolves before blockers are chosen. A creature " +
                "dealt lethal damage this way won't be around to block."
        )
        ruling(
            "2016-07-13",
            "If you transform Smoldering Werewolf into Erupting Dreadwolf after it has attacked, " +
                "Erupting Dreadwolf's triggered ability won't trigger that combat."
        )
    }
}

private val EruptingDreadwolf = card("Erupting Dreadwolf") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Werewolf"
    power = 6
    toughness = 4
    oracleText = "Whenever this creature attacks, it deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.Attacks
        val victim = target("any target", AnyTarget())
        effect = Effects.DealDamage(2, victim)
        description = "Whenever this creature attacks, it deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Zack Stella"
        flavorText = "\". . . heck, I'd settle for anything that even resembled a werewolf.\"\n" +
            "—Raf Gyel of the Quiver of Kessig"
        imageUri = "https://cards.scryfall.io/normal/back/0/b/0b0eab47-af62-4ee8-99cf-a864fadade2d.jpg?1783937460"
    }
}

val SmolderingWerewolf: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = SmolderingWerewolfFront,
    backFace = EruptingDreadwolf,
)
