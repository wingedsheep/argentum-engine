package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.disturb
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Malevolent Hermit // Benevolent Geist (Innistrad: Midnight Hunt #61)
 *
 * Front — Malevolent Hermit ({1}{U}, Creature — Human Wizard, 2/1)
 *   {U}, Sacrifice this creature: Counter target noncreature spell unless its controller pays {3}.
 *   Disturb {2}{U}
 *
 * Back — Benevolent Geist (Creature — Spirit Wizard, 2/2, blue color indicator)
 *   Flying
 *   Noncreature spells you control can't be countered.
 *   If Benevolent Geist would be put into a graveyard from anywhere, exile it instead.
 *
 * Implementation: the counter is Vodalian Mage's [Effects.CounterUnlessPays] over a noncreature
 * spell target, paid for with {U} plus sacrificing the Hermit. The back face's protection is a
 * battlefield [GrantCantBeCountered] scoped to noncreature spells its controller controls; the
 * exile-instead clause is the self-only disturb [RedirectZoneChange].
 */
private val MalevolentHermitFront = card("Malevolent Hermit") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "{U}, Sacrifice this creature: Counter target noncreature spell unless its controller pays {3}.\n" +
        "Disturb {2}{U} (You may cast this card from your graveyard transformed for its disturb cost.)"

    activatedAbility {
        target(TargetFilter.NoncreatureSpellOnStack)
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.SacrificeSelf)
        effect = Effects.CounterUnlessPays("{3}")
    }

    disturb("{2}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e79269af-63eb-43d2-afee-c38fa14a0c5b.jpg?1783925642"
        ruling(
            "2021-09-24",
            "When you cast a spell using a card's disturb ability, the card is put onto the stack " +
                "with its back face up. The resulting spell has all the characteristics of that face."
        )
        ruling(
            "2021-09-24",
            "The mana value of a spell cast using disturb is determined by the mana cost on the " +
                "front face of the card, no matter what the total cost to cast the spell was."
        )
        ruling(
            "2021-09-24",
            "The back face of each card with disturb has an ability that instructs its controller " +
                "to exile if it would be put into a graveyard from anywhere. This includes going " +
                "to the graveyard from the stack, so if the spell is countered after you cast it " +
                "using the disturb ability, it will be put into exile."
        )
    }
}

private val BenevolentGeist = card("Benevolent Geist") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U"
    typeLine = "Creature — Spirit Wizard"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Noncreature spells you control can't be countered.\n" +
        "If Benevolent Geist would be put into a graveyard from anywhere, exile it instead."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantCantBeCountered(filter = GameObjectFilter.Noncreature.youControl())
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(to = Zone.GRAVEYARD),
            selfOnly = true,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Daarken"
        flavorText = "All his old grudges suddenly seemed laughably petty. So he laughed."
        imageUri = "https://cards.scryfall.io/normal/back/e/7/e79269af-63eb-43d2-afee-c38fa14a0c5b.jpg?1783925642"
    }
}

val MalevolentHermit: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = MalevolentHermitFront,
    backFace = BenevolentGeist,
)
