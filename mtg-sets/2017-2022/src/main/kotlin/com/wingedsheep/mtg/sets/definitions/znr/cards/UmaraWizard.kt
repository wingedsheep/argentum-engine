package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Umara Wizard {4}{U} // Umara Skyfalls — Zendikar Rising #86
 * Creature — Merfolk Wizard 4/3 // Land · Modal double-faced card
 *
 * Front — Umara Wizard: "Whenever you cast an instant, sorcery, or Wizard spell, this creature
 * gains flying until end of turn."
 * Back — Umara Skyfalls: "This land enters tapped. {T}: Add {U}."
 */
private val UmaraWizardFront = card("Umara Wizard") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Whenever you cast an instant, sorcery, or Wizard spell, this creature gains flying until end of turn."
    power = 4
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery or GameObjectFilter.Any.withSubtype("Wizard")
        )
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
        description = "Whenever you cast an instant, sorcery, or Wizard spell, this creature gains " +
            "flying until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Jesper Ejsing"
        flavorText = "\"You see a waterfall. I see a way up.\"\n—Ko Dari, expedition wizard"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/890eee8d-a339-4143-adfa-1b17ec10c099.jpg?1783929391"
        ruling("2020-09-25", "An ability that triggers when a player casts a spell resolves before the spell that caused it to trigger, but after targets have been chosen for that spell (if it has any targets). The ability resolves even if that spell is countered.")
        ruling("2020-09-25", "A Wizard spell is one with the creature type Wizard. Spells that are Wizard-themed (such as Relic Amulet) aren't Wizard spells.")
    }
}

private val UmaraSkyfallsBack = card("Umara Skyfalls") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "This land enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "86"
        artist = "Jesper Ejsing"
        flavorText = "\"The source of the Umara River sits high above Oran-Rief, ever weeping on the jungle below.\"\n—Ko Dari, expedition wizard"
        imageUri = "https://cards.scryfall.io/normal/back/8/9/890eee8d-a339-4143-adfa-1b17ec10c099.jpg?1783929391"
    }
}

val UmaraWizard: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = UmaraWizardFront,
    backFace = UmaraSkyfallsBack,
)
