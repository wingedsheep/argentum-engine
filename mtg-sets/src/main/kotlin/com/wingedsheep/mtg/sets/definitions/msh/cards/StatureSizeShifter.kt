package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedWhilePropertyAtMost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Stature, Size Shifter — Marvel Super Heroes #76 (uncommon)
 * {U} · Legendary Creature — Human Hero · 1/1
 *
 * Stature can't be blocked if her power is 1 or less.
 * Power-up — {X}{U}{U}: Put X +1/+1 counters on Stature. (Activate each power-up ability only
 * once. Reduce the cost by her mana cost if she entered this turn.)
 *
 * The card is a deliberate tension and both halves have to be modelled exactly for it to read
 * right: growing her with her own power-up is what *turns off* her evasion. That rules out the
 * obvious spelling — a `ConditionalStaticAbility` wrapping `CantBeBlocked` would read her *printed*
 * power and never switch off, because the grant is Layer 6 and power is settled in Layer 7. It is
 * [CantBeBlockedWhilePropertyAtMost] instead, which the projector resolves in a post-layer pass over
 * final power and re-asks on every projection, so the evasion also comes back if she shrinks again.
 * Same ability as Tetsuko Umezawa, Fugitive, narrowed to power-only and to herself.
 *
 * The only `{X}` power-up in the set. Cost reduction never touches `{X}` (CR 601.2f applies
 * reductions to the total cost after X is announced, and the reduction here has no generic
 * component anyway): `{X}{U}{U}` − `{U}` = `{X}{U}`, so the turn she lands every mana past the
 * first blue goes straight into counters.
 */
val StatureSizeShifter = card("Stature, Size Shifter") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Hero"
    oracleText = "Stature can't be blocked if her power is 1 or less.\n" +
        "Power-up — {X}{U}{U}: Put X +1/+1 counters on Stature. (Activate each power-up ability " +
        "only once. Reduce the cost by her mana cost if she entered this turn.)"
    power = 1
    toughness = 1

    staticAbility {
        ability = CantBeBlockedWhilePropertyAtMost(
            maxValue = 1,
            properties = setOf(EntityNumericProperty.Power),
            filter = GroupFilter.source()
        )
    }

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{X}{U}{U}")
        effect = Effects.AddDynamicCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            DynamicAmount.XValue,
            EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Mintautas Šukys"
        flavorText = "\"Aw, cute! Fun-sized Dooms!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe692959-64ca-4065-9f9e-1abe590e3d0f.jpg?1783902951"
    }
}
