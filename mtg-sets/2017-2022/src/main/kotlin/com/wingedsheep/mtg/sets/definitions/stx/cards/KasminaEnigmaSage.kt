package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.LoyaltyAbilityBuilder
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedLoyaltyAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kasmina, Enigma Sage — Strixhaven: School of Mages #196 (canonical printing)
 * {1}{G}{U} · Legendary Planeswalker — Kasmina · Loyalty 2
 *
 * Each other planeswalker you control has the loyalty abilities of Kasmina.
 * +2: Scry 1.
 * −X: Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it.
 * −8: Search your library for an instant or sorcery card that shares a color with this
 * planeswalker, exile that card, then shuffle. You may cast that card without paying its mana cost.
 *
 * Kasmina's own three loyalty abilities, plus one [GrantActivatedAbility] static per ability
 * handing a copy to each *other* planeswalker you control (`excludeSelf`), the Way of the
 * Wildspeaker shape. A granted loyalty ability keeps every loyalty rule — sorcery timing, one
 * loyalty activation per planeswalker per turn, and a cost paid in the host's own loyalty.
 *
 * "This planeswalker" in the −8 is the permanent the ability is activated from: the library
 * filter reads `sharingColorWith(EffectTarget.Self)`, which names the granted ability's host
 * rather than Kasmina (ruling), and a colorless host shares no color, so nothing can be found.
 * The card is searched for, exiled, the library shuffled, and then cast for free only if the
 * player chooses to ([Effects.May] around [Effects.CastFromCollectionWithoutPayingCost]); a card
 * left uncast stays in exile.
 */
private fun kasminaScry(): Effect = Effects.Scry(1)

private fun kasminaFractal(): Effect =
    Effects.CreateToken(
        power = 0,
        toughness = 0,
        colors = setOf(Color.GREEN, Color.BLUE),
        creatureTypes = setOf(Subtype.FRACTAL.value)
    ) then
        Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.xValue(),
            EffectTarget.PipelineTarget(CREATED_TOKENS, 0)
        )

private fun kasminaUltimate(): Effect = Effects.Pipeline {
    val kasminaSearch = gather(
        CardSource.FromZone(
            Zone.LIBRARY,
            Player.You,
            GameObjectFilter.InstantOrSorcery.sharingColorWith(EffectTarget.Self)
        ),
        search = true
    )
    val kasminaFound = chooseUpTo(
        1,
        from = kasminaSearch,
        prompt = "Search for an instant or sorcery card that shares a color with this planeswalker"
    )
    val kasminaExiled = moveTracked(kasminaFound, CardDestination.ToZone(Zone.EXILE))
    run(Effects.ShuffleLibrary())
    run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(kasminaExiled)))
}

private const val KASMINA_SCRY_TEXT = "Scry 1."
private const val KASMINA_FRACTAL_TEXT =
    "Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it."
private const val KASMINA_ULTIMATE_TEXT =
    "Search your library for an instant or sorcery card that shares a color with this " +
        "planeswalker, exile that card, then shuffle. You may cast that card without paying its " +
        "mana cost."

private val otherPlaneswalkersYouControl =
    GroupFilter(GameObjectFilter.Planeswalker.youControl(), excludeSelf = true)

val KasminaEnigmaSage = card("Kasmina, Enigma Sage") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Kasmina"
    startingLoyalty = 2
    oracleText = "Each other planeswalker you control has the loyalty abilities of Kasmina.\n" +
        "+2: Scry 1.\n" +
        "−X: Create a 0/0 green and blue Fractal creature token. Put X +1/+1 counters on it.\n" +
        "−8: Search your library for an instant or sorcery card that shares a color with this " +
        "planeswalker, exile that card, then shuffle. You may cast that card without paying its " +
        "mana cost."

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedLoyaltyAbility(+2) {
                effect = kasminaScry()
                description = KASMINA_SCRY_TEXT
            },
            filter = otherPlaneswalkersYouControl
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = LoyaltyAbilityBuilder(AbilityCost.LoyaltyX).apply {
                effect = kasminaFractal()
                description = KASMINA_FRACTAL_TEXT
            }.build(),
            filter = otherPlaneswalkersYouControl
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedLoyaltyAbility(-8) {
                effect = kasminaUltimate()
                description = KASMINA_ULTIMATE_TEXT
            },
            filter = otherPlaneswalkersYouControl
        )
    }

    loyaltyAbility(+2) {
        effect = kasminaScry()
        description = KASMINA_SCRY_TEXT
    }

    loyaltyAbilityX {
        effect = kasminaFractal()
        description = KASMINA_FRACTAL_TEXT
    }

    loyaltyAbility(-8) {
        effect = kasminaUltimate()
        description = KASMINA_ULTIMATE_TEXT
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "196"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/178bf300-38cf-4ab2-9e66-6fcaa112b649.jpg?1783927310"
        ruling("2021-04-16", "Each other planeswalker you control gains Kasmina's loyalty abilities in addition to their own. For each planeswalker you control, you may still activate only one of that planeswalker's loyalty abilities per turn.")
        ruling("2021-04-16", "When you activate Kasmina's last loyalty ability on another planeswalker, you search your library for a card that shares a color with that planeswalker, not with Kasmina. You can't search for a colorless card, even if the planeswalker is colorless.")
    }
}
