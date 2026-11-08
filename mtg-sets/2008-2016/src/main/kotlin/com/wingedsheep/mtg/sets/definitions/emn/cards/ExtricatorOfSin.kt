package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Extricator of Sin // Extricator of Flesh (Eldritch Moon #23)
 *
 * Front: {2}{W} Creature — Human Cleric 0/3
 * When this creature enters, you may sacrifice another permanent. If you do, create a 3/2
 * colorless Eldrazi Horror creature token.
 * Delirium — At the beginning of your upkeep, if there are four or more card types among cards in
 * your graveyard, transform this creature.
 *
 * Back: Creature — Eldrazi Horror 3/5 (colorless)
 * Eldrazi you control have vigilance.
 * {2}, {T}, Sacrifice a non-Eldrazi creature: Create a 3/2 colorless Eldrazi Horror creature token.
 *
 * - The ETB sacrifice happens on resolution: [Effects.May] over [Effects.IfYouDo] with
 *   [SuccessCriterion.PermanentsSacrificed], so declining — or having no other permanent — makes no
 *   token. `excludeSource` keeps the Extricator itself off the list ("another permanent"), and a
 *   single sacrifice makes a single token (ruling).
 * - The delirium transform is an intervening-if: checked as the upkeep begins and again on
 *   resolution.
 * - No token `imageUri`: Eldritch Moon's Eldrazi Horror comes from the set's `temn` token art.
 */
private val ExtricatorOfSinFront = card("Extricator of Sin") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 0
    toughness = 3
    oracleText = "When this creature enters, you may sacrifice another permanent. If you do, create a " +
        "3/2 colorless Eldrazi Horror creature token.\n" +
        "Delirium — At the beginning of your upkeep, if there are four or more card types among cards " +
        "in your graveyard, transform this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.SacrificeOwn(GameObjectFilter.Permanent, excludeSource = true),
                then = Effects.CreateToken(
                    power = 3,
                    toughness = 2,
                    colors = emptySet(),
                    creatureTypes = setOf("Eldrazi", "Horror"),
                ),
                successCriterion = SuccessCriterion.PermanentsSacrificed,
            )
        )
        description = "When this creature enters, you may sacrifice another permanent. If you do, " +
            "create a 3/2 colorless Eldrazi Horror creature token."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.Delirium(4)
        effect = Effects.Transform(EffectTarget.Self)
        description = "Delirium — At the beginning of your upkeep, if there are four or more card " +
            "types among cards in your graveyard, transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6867ddd-f953-41c6-ba36-86ae2c14c908.jpg?1783937524"
        ruling(
            "2016-07-13",
            "While resolving Extricator of Sin's first ability, you can't sacrifice multiple permanents " +
                "to get multiple Eldrazi Horror tokens."
        )
    }
}

private val ExtricatorOfFlesh = card("Extricator of Flesh") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Creature — Eldrazi Horror"
    power = 3
    toughness = 5
    oracleText = "Eldrazi you control have vigilance.\n" +
        "{2}, {T}, Sacrifice a non-Eldrazi creature: Create a 3/2 colorless Eldrazi Horror creature token."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.ELDRAZI).youControl()),
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.notSubtype(Subtype.ELDRAZI)),
        )
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = emptySet(),
            creatureTypes = setOf("Eldrazi", "Horror"),
        )
        description = "{2}, {T}, Sacrifice a non-Eldrazi creature: Create a 3/2 colorless Eldrazi " +
            "Horror creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Craig J Spearing"
        flavorText = "After the end, a new beginning."
        imageUri = "https://cards.scryfall.io/normal/back/b/6/b6867ddd-f953-41c6-ba36-86ae2c14c908.jpg?1783937524"
    }
}

val ExtricatorOfSin: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ExtricatorOfSinFront,
    backFace = ExtricatorOfFlesh,
)
