package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tocasia, Dig Site Mentor
 * {1}{G}{W}{U}
 * Legendary Creature — Human Artificer
 * 4/3
 *
 * Creatures you control have vigilance and "{T}: Surveil 1."
 * {2}{G}{G}{W}{W}{U}{U}, Exile this card from your graveyard: Return any number of target
 * artifact cards with total mana value 10 or less from your graveyard to the battlefield.
 * Activate only as a sorcery.
 *
 * The first line is two group grants (Brave the Sands' vigilance + Battery Bearer's granted
 * tap ability). The graveyard ability is Spectral Steel's exile-self-from-graveyard cost over
 * Scout for Survivors' "any number of target cards with total mana value N or less" reanimation:
 * an `unlimited` target slot whose summed mana value the target validator caps at 10 (CR 601.2c).
 */
val TocasiaDigSiteMentor = card("Tocasia, Dig Site Mentor") {
    manaCost = "{1}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Human Artificer"
    power = 4
    toughness = 3
    oracleText = "Creatures you control have vigilance and \"{T}: Surveil 1.\" (To surveil 1, look at the top card of your library. You may put that card into your graveyard.)\n" +
        "{2}{G}{G}{W}{W}{U}{U}, Exile this card from your graveyard: Return any number of target artifact cards with total mana value 10 or less from your graveyard to the battlefield. Activate only as a sorcery."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.Surveil(1)
            ),
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}{G}{W}{W}{U}{U}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        targets(
            TargetFilter.ArtifactInYourGraveyard,
            unlimited = true,
            totalManaValueAtMost = DynamicAmounts.fixed(10),
        )
        effect = Effects.Pipeline {
            val artifacts = gather(CardSource.ChosenTargets)
            move(artifacts, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/919a32a6-b66d-4a75-a02f-fa1c4c87b738.jpg?1783920024"
    }
}
