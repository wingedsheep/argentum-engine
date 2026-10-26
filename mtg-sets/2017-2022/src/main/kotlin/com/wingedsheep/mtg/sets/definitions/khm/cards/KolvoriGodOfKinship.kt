package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kolvori, God of Kinship // The Ringhart Crest — Kaldheim #181 (canonical printing)
 * Legendary Creature — God 2/4 // Legendary Artifact · Modal double-faced card
 *
 * Back: the Crest's {G} carries `AnyOf(OfChosenType(creatureOnly), LegendarySpellsOnly(creatureOnly))`;
 * the engine binds the chosen-type half to the type chosen as the Crest entered. With no type chosen
 * that half stays unbound and the mana can still pay for a legendary creature spell (ruling).
 */
private val legendaryCreatures = GameObjectFilter.Creature.legendary()

private val KolvoriGodOfKinshipFront = card("Kolvori, God of Kinship") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — God"
    power = 2
    toughness = 4
    oracleText = "As long as you control three or more legendary creatures, Kolvori gets +4/+2 and has vigilance.\n" +
        "{1}{G}, {T}: Look at the top six cards of your library. You may reveal a legendary creature card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 4, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.YouControlAtLeast(3, legendaryCreatures),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source()),
            condition = Conditions.YouControlAtLeast(3, legendaryCreatures),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 6,
            filter = legendaryCreatures,
            prompt = "You may reveal a legendary creature card from among them and put it into your hand",
        )
        description = "Look at the top six cards of your library. You may reveal a legendary creature card " +
            "from among them and put it into your hand. Put the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b76bed98-30b1-4572-b36c-684ada06826c.jpg?1783928215"
        ruling("2021-02-05", "Because damage remains marked on creatures until the damage is removed as the turn ends, nonlethal damage dealt to Kolvori may become lethal if you controlled three or more legendary creatures and some of them leave the battlefield that turn.")
    }
}

private val TheRinghartCrestBack = card("The Ringhart Crest") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact"
    oracleText = "As The Ringhart Crest enters, choose a creature type.\n" +
        "{T}: Add {G}. Spend this mana only to cast a creature spell of the chosen type or a legendary creature spell."

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.GREEN,
            restriction = ManaRestriction.AnyOf(
                listOf(
                    ManaRestriction.OfChosenType(creatureOnly = true),
                    ManaRestriction.LegendarySpellsOnly(creatureOnly = true),
                )
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Grzegorz Rutkowski"
        flavorText = "Kolvori's love for her family knows no bounds."
        imageUri = "https://cards.scryfall.io/normal/back/b/7/b76bed98-30b1-4572-b36c-684ada06826c.jpg?1783928215"
        ruling("2021-02-05", "For The Ringhart Crest's first ability, you must choose an existing creature type, such as Fungus or Archer. You can't choose card types (e.g., artifact) or supertypes (e.g., snow).")
        ruling("2021-02-05", "If The Ringhart Crest is somehow on the battlefield with no choice of creature type, the mana produced by its activated ability can still be spent to cast a legendary creature spell.")
    }
}

val KolvoriGodOfKinship: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = KolvoriGodOfKinshipFront,
    backFace = TheRinghartCrestBack,
)
