package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Curious Homunculus // Voracious Reader (Eldritch Moon #54 — the card's earliest printing; also
 * reprinted in Shadows over Innistrad Remastered)
 * {1}{U}
 * Creature — Homunculus 1/1 // Creature — Eldrazi Homunculus 3/4
 *
 * Front — Curious Homunculus
 *   {T}: Add {C}. Spend this mana only to cast an instant or sorcery spell.
 *   At the beginning of your upkeep, if there are three or more instant and/or sorcery cards in your
 *   graveyard, transform this creature.
 *
 * Back — Voracious Reader (colorless Eldrazi, no color indicator)
 *   Prowess
 *   Instant and sorcery spells you cast cost {1} less to cast.
 *
 * The upkeep trigger is an intervening "if" (CR 603.4): `interveningIf` checks the graveyard count
 * both when the step begins and again on resolution, matching the printed ruling. The back's cost
 * reduction is the Goblin Electromancer static — generic only, applied after cost increases.
 */

private val CuriousHomunculusFront = card("Curious Homunculus") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homunculus"
    power = 1
    toughness = 1
    oracleText = "{T}: Add {C}. Spend this mana only to cast an instant or sorcery spell.\n" +
        "At the beginning of your upkeep, if there are three or more instant and/or sorcery cards " +
        "in your graveyard, transform this creature."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1, restriction = ManaRestriction.InstantOrSorceryOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CardsInGraveyardMatchingAtLeast(3, GameObjectFilter.InstantOrSorcery)
        effect = Effects.Transform(EffectTarget.Self)
        description = "At the beginning of your upkeep, if there are three or more instant and/or " +
            "sorcery cards in your graveyard, transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22e816af-df55-4a3f-a6e7-0ff3bb1b45b5.jpg?1783937507"
        ruling(
            "2016-07-13",
            "If you don't have enough instant and/or sorcery cards in your graveyard as your upkeep " +
                "begins, Curious Homunculus's last ability doesn't trigger. If you don't have enough as " +
                "the ability resolves, the ability has no effect."
        )
        ruling(
            "2016-07-13",
            "Mana from Curious Homunculus's first ability can be spent to cast a spell that has generic " +
                "mana in its additional cost, such as Borrowed Malevolence with both modes chosen. It " +
                "can't be spent to pay costs during a spell's resolution, such as the cost Convolute " +
                "prompts a player to pay."
        )
        ruling(
            "2016-07-13",
            "If you activate Curious Homunculus's mana ability in response to its triggered ability, " +
                "you can spend that mana to cast an instant spell during your upkeep after it has " +
                "transformed into Voracious Reader. If you don't spend the mana, it will be lost before " +
                "your draw step begins; you can't use it to cast the card you draw or to cast a sorcery."
        )
    }
}

private val VoraciousReader = card("Voracious Reader") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Homunculus"
    power = 3
    toughness = 4
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "Instant and sorcery spells you cast cost {1} less to cast."

    prowess()

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Filip Burburan"
        flavorText = "Good books make you savor each word."
        imageUri = "https://cards.scryfall.io/normal/back/2/2/22e816af-df55-4a3f-a6e7-0ff3bb1b45b5.jpg?1783937507"
        ruling(
            "2016-07-13",
            "Abilities that reduce the cost of a spell apply after abilities that raise them. For " +
                "example, Borrowed Malevolence with both modes chosen costs {1}{B} to cast if you " +
                "control Voracious Reader."
        )
        ruling(
            "2016-07-13",
            "If an instant or sorcery spell has {X} in its mana cost, choose the value for X first, " +
                "and then reduce the cost by {1}. For example, Dark Salvation with X chosen as 2 costs " +
                "{3}{B} to cast if you control Voracious Reader."
        )
    }
}

val CuriousHomunculus: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = CuriousHomunculusFront,
    backFace = VoraciousReader,
)
