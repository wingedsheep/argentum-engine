package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Katilda, Dawnhart Prime — Innistrad: Midnight Hunt #230
 * {G}{W} · Legendary Creature — Human Warlock · 1/1
 *
 * Each Human's granted mana ability reads *its own* colors (`ColorsOf(Self)` resolved against the
 * Human activating it), so a colorless Human adds nothing — the same shape Tazri, Stalwart Survivor uses.
 */
val KatildaDawnhartPrime = card("Katilda, Dawnhart Prime") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Human Warlock"
    power = 1
    toughness = 1
    oracleText = "Protection from Werewolves\n" +
        "Human creatures you control have \"{T}: Add one mana of any of this creature's colors.\"\n" +
        "{4}{G}{W}, {T}: Put a +1/+1 counter on each creature you control."

    keywordAbility(KeywordAbility.protectionFromSubtype("Werewolf"))

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddManaOfChoice(colorSet = ManaColorSet.ColorsOf(EffectTarget.Self)),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl()),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}{W}"), Costs.Tap)
        description = "Put a +1/+1 counter on each creature you control."
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Bryan Sola"
        flavorText = "\"The angels may have abandoned us, but Ghrin-Danu has not.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/673cd775-d417-4652-a91d-825ad5c89e8a.jpg?1783925559"
    }
}
