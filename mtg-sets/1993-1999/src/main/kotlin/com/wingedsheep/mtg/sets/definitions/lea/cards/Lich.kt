package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantLoseGameFromLife
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.ReplaceLifeGainWith
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

// Current Oracle makes the life-total reduction an as-enters replacement, rather than an ETB trigger.
val Lich = card("Lich") {
    manaCost = "{B}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, you lose life equal to your life total.\n" +
        "You don't lose the game for having 0 or less life.\n" +
        "If you would gain life, draw that many cards instead.\n" +
        "Whenever you're dealt damage, sacrifice that many nontoken permanents. If you can't, you lose the game.\n" +
        "When this enchantment is put into a graveyard from the battlefield, you lose the game."

    replacementEffect(OnEnterRun(Effects.LoseLife(DynamicAmounts.lifeTotal(Player.You), EffectTarget.Controller)))
    staticAbility { ability = GrantCantLoseGameFromLife }
    replacementEffect(ReplaceLifeGainWith(
        replacementEffect = Effects.DrawCards(DynamicAmounts.replacementLifeGainAmount()),
        appliesTo = EventPattern.LifeGainEvent(Player.You)
    ))
    triggeredAbility {
        trigger = Triggers.you.isDealtDamage()
        effect = Effects.Sacrifice(
            filter = GameObjectFilter.Permanent.nontoken(),
            count = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.Controller
        ) then Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.permanentsSacrificedThisWay(),
                ComparisonOperator.LT,
                DynamicAmounts.triggerDamageAmount()
            ),
            then = Effects.LoseGame()
        )
    }
    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.LoseGame()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4250caec-0e37-41be-9ec4-8938deb5f0d0.jpg?1783948694"
        ruling("2011-01-01", "The last ability will cause you to lose the game even if you somehow manage to have a life total greater than 0 at the time the Lich is put into the graveyard or if some other effect would prevent you from losing for having 0 life. If, on the other hand, some effect such as that from Platinum Angel says that you can't lose the game then even the last ability of the Lich cannot cause you to do so. The ability will just resolve and the game will continue as normal.")
        ruling("2010-08-15", "Note that usually you will have 0 or less life at the time Lich leaves the battlefield causing you to lose as a State-Based Action before the last ability can even go on the stack.")
        ruling("2004-10-04", "If an opponent steals control of Lich and no other effect prevents you from losing with a life total of zero, you will lose the game due to a zero life total as a State-Based Action before you can take any actions. The last sentence doesn't apply in this case since the Lich didn't leave the battlefield.")
        ruling("2004-10-04", "If you have multiple Lich cards on the battlefield, you must sacrifice a permanent for each damage done to you for each Lich. This is because the sacrifice is a triggered ability. But you only draw one card for each life gained regardless of how many Liches you have. This is because the draw is a replacement effect and not a triggered one. You lose if any one of the Liches leaves the battlefield.")
        ruling("2004-10-04", "If an opponent steals control of Lich, their life total does not change. The life total changes for a player only when it enters under that player's control.")
        ruling("2004-10-04", "If you take more than one damage at a time, sacrifice the permanents for that damage simultaneously. This allows you to sacrifice both a creature and any Aura that is on it all at once.")
    }
}
