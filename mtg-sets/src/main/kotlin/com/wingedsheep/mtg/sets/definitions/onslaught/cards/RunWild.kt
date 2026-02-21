package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect

/**
 * Run Wild
 * {G}
 * Instant
 * Until end of turn, target creature gains trample and "{G}: Regenerate this creature."
 */
val RunWild = card("Run Wild") {
    manaCost = "{G}"
    typeLine = "Instant"

    spell {
        val t = target("target", Targets.Creature)
        effect = GrantKeywordUntilEndOfTurnEffect(
            keyword = Keyword.TRAMPLE,
            target = t
        ) then GrantActivatedAbilityUntilEndOfTurnEffect(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Mana(ManaCost.parse("{G}")),
                effect = RegenerateEffect(EffectTarget.Self)
            ),
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "279"
        artist = "Alan Pollack"
        flavorText = "Wirewood's beasts didn't seem to mind when the elves moved in. In fact, they hardly noticed them underfoot."
        imageUri = "https://cards.scryfall.io/large/front/9/3/939a7354-162c-489d-955d-4df17b930e1c.jpg?1562929803"
    }
}
