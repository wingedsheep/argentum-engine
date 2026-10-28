package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Verazol, the Split Current — Zendikar Rising #239
 * {X}{G}{U} · Legendary Creature — Serpent · 0/0
 *
 * Verazol enters with a +1/+1 counter on it for each mana spent to cast it.
 * Whenever you cast a kicked spell, you may remove two +1/+1 counters from Verazol. If you do,
 * copy that spell. You may choose new targets for the copy.
 *
 * The counters read the total mana spent (X, the coloured pips, and any additional cost such as
 * commander tax); a Verazol that wasn't cast enters with none. Removing the two counters is a
 * [Effects.MayPay] cost, offered only while Verazol still carries both — never just one, and not
 * once it has left the battlefield. The copy is made from the triggering spell even if it has
 * since been countered (the copy executor falls back to its last stack state), and a copied
 * permanent spell becomes a token.
 */
val VerazolTheSplitCurrent = card("Verazol, the Split Current") {
    manaCost = "{X}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Serpent"
    oracleText = "Verazol enters with a +1/+1 counter on it for each mana spent to cast it.\n" +
        "Whenever you cast a kicked spell, you may remove two +1/+1 counters from Verazol. If you do, " +
        "copy that spell. You may choose new targets for the copy. " +
        "(A copy of a permanent spell becomes a token.)"
    power = 0
    toughness = 0

    replacementEffect(
        EntersWithDynamicCounters(count = DynamicAmounts.totalManaSpent())
    )

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
        effect = Effects.MayPay(
            cost = Effects.RemoveCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
            then = Effects.CopyTargetSpell(EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "239"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/752a2aa9-40ed-4c64-b945-bc6fec606b7b.jpg?1783929318"
        ruling("2020-09-25", "The amount of mana you spent to cast Verazol is usually equal to its mana value, normally 2 plus the value chosen for X. However, you also include any additional costs you pay, including the \"commander tax\" in a Commander game.")
        ruling("2020-09-25", "If Verazol enters the battlefield without being cast, then no mana was spent to cast it. It enters the battlefield without any +1/+1 counters. If no other effects are increasing its toughness at that time, it will be put into its owner's graveyard as a state-based action.")
        ruling("2020-09-25", "You can't remove more than two counters as Verazol's last ability resolves to copy the spell more than once. You can't remove just one counter from it if it has only one. If Verazol leaves the battlefield while its last ability is on the stack, you can't remove two +1/+1 counters from it at all.")
        ruling("2020-09-25", "A copy is created even if the spell that caused Verazol's last ability to trigger has been countered by the time that ability resolves. The copy resolves before the original spell.")
        ruling("2020-09-25", "You can't choose to pay any additional costs for the copy. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too. Most notably, the copy is also kicked.")
    }
}
