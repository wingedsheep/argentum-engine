package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brimstone Roundup
 * {1}{R}
 * Enchantment
 *
 * Whenever you cast your second spell each turn, create a 1/1 red Mercenary creature token with
 * "{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 * Plot {2}{R}
 */
val BrimstoneRoundup = card("Brimstone Roundup") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast your second spell each turn, create a 1/1 red Mercenary " +
        "creature token with \"{T}: Target creature you control gets +1/+0 until end of turn. " +
        "Activate only as a sorcery.\"\n" +
        "Plot {2}{R} (You may pay {2}{R} and exile this card from your hand. Cast it as a sorcery " +
        "on a later turn without paying its mana cost. Plot only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.NthSpellCast(2, Player.You)
        effect = CreateTokenEffect(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Mercenary"),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = AbilityCost.Tap,
                    effect = Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.CreatureYouControl),
                    timing = TimingRule.SorcerySpeed
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f04607f-eed2-462e-897f-82e41e5f7049.jpg?1712316319"
        )
        description = "Whenever you cast your second spell each turn, create a 1/1 red Mercenary " +
            "creature token with \"{T}: Target creature you control gets +1/+0 until end of turn. " +
            "Activate only as a sorcery.\""
    }

    keywordAbility(KeywordAbility.plot("{2}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd13011e-a4fc-4107-988f-60cfc851ecd3.jpg?1712355715"
    }
}
