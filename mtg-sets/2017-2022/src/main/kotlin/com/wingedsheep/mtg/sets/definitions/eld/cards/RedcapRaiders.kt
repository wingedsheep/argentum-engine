package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Redcap Raiders — Throne of Eldraine #136
 * {2}{R} · Creature — Goblin Warrior · 3/2
 *
 * Whenever this creature attacks, you may tap an untapped non-Human creature you control. If you
 * do, this creature gets +1/+1 and gains trample until end of turn.
 *
 * Civil Servant's shape: an [Effects.MayPay] whose cost is Gather → Select-exactly-1 → Tap. No
 * "another", so Redcap Raiders itself (a Goblin) may be tapped if it is untapped on resolution —
 * e.g. with vigilance. The creature is chosen on resolution, so tapping an attacking creature with
 * vigilance doesn't remove it from combat.
 */
val RedcapRaiders = card("Redcap Raiders") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, you may tap an untapped non-Human creature you control. " +
        "If you do, this creature gets +1/+1 and gains trample until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val tapCost = Effects.Pipeline {
            val redcapRaidersTapPool = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature.notSubtype(Subtype.HUMAN).untapped(),
                    player = Player.You,
                )
            )
            val redcapRaidersToTap = chooseExactly(
                1,
                from = redcapRaidersTapPool,
                prompt = "Tap an untapped non-Human creature you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(redcapRaidersToTap, tap = true))
        }
        effect = Effects.MayPay(
            cost = tapCost,
            then = Effects.ModifyStats(1, 1, EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
            descriptionOverride = "You may tap an untapped non-Human creature you control. If you do, " +
                "this creature gets +1/+1 and gains trample until end of turn.",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Lie Setiawan"
        flavorText = "\"I admire their courage as much as I despise their savagery.\"\n—Syr Carah, the Bold"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/686cf5c4-6ba3-4fa0-9732-0a350c637c7a.jpg?1783932619"
        ruling(
            "2019-10-04",
            "Tapping an attacking creature with vigilance to pay the cost of Redcap Raiders's ability " +
                "won't cause that creature to leave combat or to stop attacking."
        )
        ruling(
            "2019-10-04",
            "Players can't take any actions between the time you choose whether to tap a creature and " +
                "the time Redcap Raiders gets +1/+1 and gains trample."
        )
    }
}
