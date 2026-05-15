package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Domri, Anarch of Bolas
 * {1}{R}{G}
 * Legendary Planeswalker — Domri
 * Starting Loyalty: 3
 *
 * Creatures you control get +1/+0.
 * +1: Add {R} or {G}. Creature spells you cast this turn can't be countered.
 * −2: Target creature you control fights target creature you don't control.
 */
val DomriAnarchOfBolas = card("Domri, Anarch of Bolas") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Planeswalker — Domri"
    startingLoyalty = 3
    oracleText = "Creatures you control get +1/+0.\n" +
        "+1: Add {R} or {G}. Creature spells you cast this turn can't be countered.\n" +
        "−2: Target creature you control fights target creature you don't control."

    // Creatures you control get +1/+0.
    staticAbility {
        ability = ModifyStats(1, 0, GroupFilter.AllCreaturesYouControl)
    }

    // +1: Add {R} or {G}. Creature spells you cast this turn can't be countered.
    loyaltyAbility(+1) {
        effect = Effects.AddManaInAnyCombination(
            amount = 1,
            allowedColors = setOf(Color.RED, Color.GREEN)
        ).then(
            Effects.GrantSpellsCantBeCountered(spellFilter = GameObjectFilter.Creature)
        )
    }

    // −2: Target creature you control fights target creature you don't control.
    loyaltyAbility(-2) {
        val yours = target("creature you control", Targets.CreatureYouControl)
        val theirs = target("creature you don't control", Targets.CreatureOpponentControls)
        // Fight requires both targets to be legal at resolution; if either is illegal,
        // no creature deals or is dealt damage (per the printed ruling on this card).
        effect = ConditionalEffect(
            condition = Conditions.All(
                Conditions.TargetMatchesFilter(
                    GameObjectFilter.Creature.youControl(), targetIndex = 0
                ),
                Conditions.TargetMatchesFilter(
                    GameObjectFilter.Creature.opponentControls(), targetIndex = 1
                )
            ),
            effect = Effects.Fight(yours, theirs)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1af9881-e35b-4be2-8716-ea7c6664e22c.jpg?1557577104"

        ruling("2019-05-03", "Because it's a loyalty ability, Domri's first loyalty ability isn't a mana ability. It can be activated only any time you could cast a sorcery. It uses the stack and can be responded to.")
        ruling("2019-05-03", "After Domri's first loyalty ability has resolved, no creature spells you cast can be countered during that turn, not just the one you spend the mana on.")
        ruling("2019-05-03", "A spell or ability that counters spells can still target a creature spell you control after Domri's first loyalty ability has resolved. When that spell or ability resolves, the creature spell won't be countered, but any additional effects of that spell or ability will still happen.")
        ruling("2019-05-03", "If Domri leaves the battlefield before his last ability resolves, most likely because he only had 2 loyalty when you activated the ability, the creature won't have +1/+0 from Domri's static ability while it fights.")
        ruling("2019-05-03", "If either target is an illegal target as Domri's last ability resolves, no creature will deal or be dealt damage.")
    }
}
