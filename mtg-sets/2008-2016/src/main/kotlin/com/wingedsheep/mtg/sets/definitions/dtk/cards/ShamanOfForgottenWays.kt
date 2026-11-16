package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shaman of Forgotten Ways
 * {2}{G}
 * Creature — Human Shaman
 * 2/3
 *
 * {T}: Add two mana in any combination of colors. Spend this mana only to cast creature spells.
 * Formidable — {9}{G}{G}, {T}: Each player's life total becomes the number of creatures they
 * control. Activate only if creatures you control have total power 8 or greater.
 *
 * The mana ability is the Great Hall of the Citadel shape with the creature-spell restriction
 * (face-down morph casts count as creature spells). The formidable ability is Biorhythm's
 * per-player life-total set — `ForEachPlayer` rebinds "you" to each player in turn, so each
 * player's own creature count is read at resolution — gated by the formidable total-power check.
 */
val ShamanOfForgottenWays = card("Shaman of Forgotten Ways") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    oracleText = "{T}: Add two mana in any combination of colors. Spend this mana only to cast creature spells.\n" +
        "Formidable — {9}{G}{G}, {T}: Each player's life total becomes the number of creatures they control. " +
        "Activate only if creatures you control have total power 8 or greater."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaInAnyCombination(2, restriction = ManaRestriction.CreatureSpellsOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{9}{G}{G}"), Costs.Tap)
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effect = Effects.SetLifeTotal(DynamicAmounts.creaturesYouControl(), EffectTarget.Controller)
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
                    ComparisonOperator.GTE,
                    8
                )
            )
        )
        description = "Formidable — {9}{G}{G}, {T}: Each player's life total becomes the number of creatures they control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "204"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3e9c8f7-2232-4a2a-8a00-0a2908bc7543.jpg?1783938576"
        ruling("2015-02-25", "The mana produced by Shaman of Forgotten Ways can be used to cast face-down creature spells with morph or megamorph.")
        ruling("2015-02-25", "Count the number of creatures each player controls when the last ability resolves to determine what number each player's life total will become.")
        ruling("2015-02-25", "For a player's life total to become a specific number, the player gains or loses the appropriate amount of life. For example, if a player's life total is 12 and that player controls three creatures as the ability resolves, they will lose 9 life. Other effects that interact with life gain or loss will interact with this effect accordingly.")
        ruling("2015-02-25", "If a player controls no creatures as the last ability resolves, their life total will become 0, causing that player to lose the game. If all players' life totals become 0 this way, the game will be a draw.")
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
