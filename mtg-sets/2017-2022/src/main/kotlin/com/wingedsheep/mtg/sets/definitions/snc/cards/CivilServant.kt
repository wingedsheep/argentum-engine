package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Civil Servant
 * {G}{W}
 * Creature — Cat Citizen
 * 2/3
 * Whenever this creature attacks, you may tap another untapped Citizen you control. If you do,
 * this creature gets +1/+0 and gains lifelink until end of turn.
 *
 * "You may tap another untapped Citizen … If you do" is an [Effects.MayPay] whose payable cost is
 * the Gather → Select-exactly-1 → Tap pipeline (same shape as Caparocti Sunborn); `excludeSelf`
 * carries the "another", so Civil Servant can't tap itself. The Citizen needn't be a creature, and
 * summoning sickness doesn't matter — it isn't a {T} cost.
 */
val CivilServant = card("Civil Servant") {
    manaCost = "{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Cat Citizen"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature attacks, you may tap another untapped Citizen you control. " +
        "If you do, this creature gets +1/+0 and gains lifelink until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val tapCost = Effects.Pipeline {
            val civilServantTapPool = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Permanent.withSubtype("Citizen").untapped(),
                    player = Player.You,
                    excludeSelf = true,
                )
            )
            val civilServantToTap = chooseExactly(
                1,
                from = civilServantTapPool,
                prompt = "Tap another untapped Citizen you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(civilServantToTap, tap = true))
        }
        effect = Effects.MayPay(
            cost = tapCost,
            then = Effects.ModifyStats(1, 0, EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self),
            descriptionOverride = "You may tap another untapped Citizen you control. If you do, " +
                "this creature gets +1/+0 and gains lifelink until end of turn.",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Tony Foti"
        flavorText = "\"Together we can build a safer, brighter New Capenna!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cf5da68-a79b-40a3-93bc-785fb2352b05.jpg?1783923091"
    }
}
