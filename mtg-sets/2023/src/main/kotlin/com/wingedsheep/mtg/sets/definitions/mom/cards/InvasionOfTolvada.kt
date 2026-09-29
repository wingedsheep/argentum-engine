package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Invasion of Tolvada // The Broken Sky — March of the Machine #241.
 * {3}{W}{B} · Battle — Siege · defense 5 // Enchantment
 *
 * Front: the Siege's enter trigger returns target nonbattle permanent card from your graveyard to
 * the battlefield. Back: creature tokens you control get +1/+0 and have lifelink (all of them, not
 * only the Spirits it makes), and a 1/1 white and black flying Spirit at your end step.
 */
private val InvasionOfTolvadaFront = card("Invasion of Tolvada") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, return target nonbattle permanent card from your graveyard to " +
        "the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(
            TargetFilter(
                GameObjectFilter.Permanent.ownedByYou()
                    .withCardPredicate(CardPredicate.Not(CardPredicate.IsBattle)),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "When this Siege enters, return target nonbattle permanent card from your " +
            "graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Henry Peters"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00255899-aaaf-46c6-8037-bd0e3c06250c.jpg?1783916950"
    }
}

private val TheBrokenSky = card("The Broken Sky") {
    manaCost = ""
    colorIdentity = "WB"
    colorIndicator = "WB"
    typeLine = "Enchantment"
    oracleText = "Creature tokens you control get +1/+0 and have lifelink.\n" +
        "At the beginning of your end step, create a 1/1 white and black Spirit creature token " +
        "with flying."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.token().youControl()),
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.LIFELINK, GroupFilter(GameObjectFilter.Creature.token().youControl()))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/6/d/6d02a57e-6c76-491c-85f8-8f4d825be2c2.jpg?1783916669",
        )
        description = "At the beginning of your end step, create a 1/1 white and black Spirit " +
            "creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Henry Peters"
        flavorText = "Angry ghosts poured through and pummeled the Invasion Tree, filling the " +
            "heavens with the rhythms of war."
        imageUri = "https://cards.scryfall.io/normal/back/0/0/00255899-aaaf-46c6-8037-bd0e3c06250c.jpg?1783916950"
        ruling("2023-04-14", "The Broken Sky's first ability applies to all creature tokens you control, not only the ones created by its other ability.")
    }
}

val InvasionOfTolvada: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfTolvadaFront,
    backFace = TheBrokenSky,
)
