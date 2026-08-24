package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wormwood Treefolk
 * {3}{G}{G}
 * Creature — Treefolk
 * 4/4
 * {G}{G}: This creature gains forestwalk until end of turn and deals 2 damage to you.
 * {B}{B}: This creature gains swampwalk until end of turn and deals 2 damage to you.
 *
 * Two symmetrical activated abilities, each a composite of a self-targeted
 * [Effects.GrantKeyword] for [Duration.EndOfTurn] and 2 damage to the ability's controller. The
 * damage is dealt *by* the Treefolk (it is the source), which matters for damage prevention and
 * redirection reading the source.
 */
val WormwoodTreefolk = card("Wormwood Treefolk") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Treefolk"
    power = 4
    toughness = 4
    oracleText = "{G}{G}: This creature gains forestwalk until end of turn and deals 2 damage to " +
        "you. (It can't be blocked as long as defending player controls a Forest.)\n" +
        "{B}{B}: This creature gains swampwalk until end of turn and deals 2 damage to you. " +
        "(It can't be blocked as long as defending player controls a Swamp.)"

    activatedAbility {
        cost = Costs.Mana("{G}{G}")
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.FORESTWALK, EffectTarget.Self, Duration.EndOfTurn),
            Effects.DealDamage(2, EffectTarget.PlayerRef(Player.You)),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{B}{B}")
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.SWAMPWALK, EffectTarget.Self, Duration.EndOfTurn),
            Effects.DealDamage(2, EffectTarget.PlayerRef(Player.You)),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fa20173-e88a-4b14-9c54-14567ca5571c.jpg?1783947928"
    }
}
