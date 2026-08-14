package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodsworn Squire // Bloodsworn Knight (Innistrad: Crimson Vow)
 * {3}{B}
 * Creature — Vampire Soldier // Creature — Vampire Knight
 *
 * Front — Bloodsworn Squire (3/3)
 *   {1}{B}, Discard a card: This creature gains indestructible until end of turn. Tap it. Then if
 *   there are four or more creature cards in your graveyard, transform this creature.
 *
 * Back — Bloodsworn Knight (P/T = creature cards in your graveyard)
 *   Bloodsworn Knight's power and toughness are each equal to the number of creature cards in your
 *   graveyard.
 *   {1}{B}, Discard a card: This creature gains indestructible until end of turn. Tap it.
 *
 * The front's activated ability pays `{1}{B}` plus discarding a card ([Costs.Composite]) for a
 * [Effects.Composite] of grant-indestructible-until-EOT, tap itself, and a [ConditionalEffect] gated
 * on [Conditions.CreatureCardsInGraveyardAtLeast] 4 that transforms it (Immersturm Predator's
 * indestructible-then-tap idiom). The back's characteristic-defining P/T is a self-referential
 * [SetBasePowerToughnessDynamicStatic] CDA counting creature cards in your graveyard, and it repeats
 * the same activated ability without the transform clause. The back is a transformed face with no
 * mana cost, so its color comes from a color indicator (CR 204): `colorIndicator = "B"`.
 */

/** Number of creature cards in your graveyard — the Knight's characteristic-defining P/T. */
private val creatureCardsInGraveyard = DynamicAmounts.creatureCardsInYourGraveyard()

private val BloodswornSquireFront = card("Bloodsworn Squire") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    power = 3
    toughness = 3
    oracleText = "{1}{B}, Discard a card: This creature gains indestructible until end of turn. Tap " +
        "it. Then if there are four or more creature cards in your graveyard, transform this creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Discard())
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
            Effects.Tap(EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.CreatureCardsInGraveyardAtLeast(4),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "This creature gains indestructible until end of turn. Tap it. Then if there " +
            "are four or more creature cards in your graveyard, transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Darren Tan"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7cbdd54-7685-4921-ab60-dc36e647a4c5.jpg?1783924883"
    }
}

private val BloodswornKnight = card("Bloodsworn Knight") {
    manaCost = ""
    colorIdentity = "B"
    colorIndicator = "B" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Vampire Knight"
    power = 0
    toughness = 0
    oracleText = "Bloodsworn Knight's power and toughness are each equal to the number of creature " +
        "cards in your graveyard.\n" +
        "{1}{B}, Discard a card: This creature gains indestructible until end of turn. Tap it."

    staticAbility {
        ability = SetBasePowerToughnessDynamicStatic(
            power = creatureCardsInGraveyard,
            toughness = creatureCardsInGraveyard,
            filter = GroupFilter.source(),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Discard())
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
            Effects.Tap(EffectTarget.Self),
        )
        description = "This creature gains indestructible until end of turn. Tap it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Darren Tan"
        imageUri = "https://cards.scryfall.io/normal/back/a/7/a7cbdd54-7685-4921-ab60-dc36e647a4c5.jpg?1783924883"
    }
}

val BloodswornSquire: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BloodswornSquireFront,
    backFace = BloodswornKnight,
)
