package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Winterthorn Blessing
 * {G}{U}
 * Sorcery
 * Put a +1/+1 counter on up to one target creature you control. Tap up to one target creature you
 * don't control, and that creature doesn't untap during its controller's next untap step.
 * Flashback {1}{G}{U}
 *
 * Two independent "up to one" slots, the Explosive Entry shape: either may be left empty, and an
 * effect aimed at an unchosen (or since-illegal) slot simply does nothing while the other still
 * resolves. The freeze is the Berg Strider / Entangling Trap shape — tap, then
 * [AbilityFlag.DOESNT_UNTAP] for [Duration.UntilAfterAffectedControllersNextUntap] on the same slot.
 * "You don't control" is the house [TargetFilter.Creature.opponentControls] reading (cf. Clear Shot).
 */
val WinterthornBlessing = card("Winterthorn Blessing") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on up to one target creature you control. Tap up to one target " +
        "creature you don't control, and that creature doesn't untap during its controller's next " +
        "untap step.\n" +
        "Flashback {1}{G}{U} (You may cast this card from your graveyard for its flashback cost. " +
        "Then exile it.)"

    spell {
        val yours = target(TargetFilter.Creature.youControl(), optional = true)
        val theirs = target(TargetFilter.Creature.opponentControls(), optional = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, yours) then
            Effects.Tap(theirs) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                theirs,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
    }

    keywordAbility(KeywordAbility.flashback("{1}{G}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1e9edd3-151d-4bf6-b491-03db9db32234.jpg?1783925550"
    }
}
