package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Ignoble Hierarch — Modern Horizons 2 #166
 * {G} · Creature — Goblin Shaman · 0/1
 *
 * Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)
 * {T}: Add {B}, {R}, or {G}.
 *
 * Printed exalted is the bare keyword; the engine derives the trigger from it
 * (see [com.wingedsheep.sdk.scripting.Exalted]). The three-colour mana ability is three
 * separate {T} mana abilities, the shape every Jund tri-land uses.
 */
val IgnobleHierarch = card("Ignoble Hierarch") {
    manaCost = "{G}"
    colorIdentity = "BRG"
    typeLine = "Creature — Goblin Shaman"
    oracleText = "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 " +
        "until end of turn.)\n{T}: Add {B}, {R}, or {G}."
    power = 0
    toughness = 1

    keywords(Keyword.EXALTED)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Mark Zug"
        flavorText = "He protects the fetid bog from light, life, and the hideous sound of laughter."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/aba51852-af8f-49d8-8fb6-22d52a1742b8.jpg?1783926830"

        ruling(
            "2021-06-18",
            "A creature attacks alone if it's the only creature declared as an attacker during the " +
                "declare attackers step (including creatures controlled by your teammates, if " +
                "applicable). For example, exalted won't trigger if you attack with multiple creatures " +
                "and all but one of them are removed from combat."
        )
    }
}
