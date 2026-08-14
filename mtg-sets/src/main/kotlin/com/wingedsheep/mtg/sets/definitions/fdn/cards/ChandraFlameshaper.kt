package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Chandra, Flameshaper
 * {5}{R}{R}
 * Legendary Planeswalker — Chandra
 * Loyalty 6
 *
 * +2: Add {R}{R}{R}. Exile the top three cards of your library. Choose one. You may play that card
 *     this turn.
 * +1: Create a token that's a copy of target creature you control, except it has haste and
 *     "At the beginning of the end step, sacrifice this token."
 * −4: Chandra deals 8 damage divided as you choose among any number of target creatures and/or
 *     planeswalkers.
 *
 * Modeling notes:
 *  - The +2 is **not** a mana ability (printed ruling): it has other effects and is a loyalty
 *    ability, so it uses the stack. That falls out for free — `isManaAbility` is author-declared
 *    and left off, so the enumerator treats it as an ordinary non-mana ability.
 *  - "Exile the top three … Choose one. You may play that card" is a gather → exile → choose →
 *    grant pipeline, with the play permission granted only to the *chosen* card; the other two stay
 *    exiled with no permission. The choice happens after the move, so the player is picking among
 *    cards that are already in exile. Same shape as End-Blaze Epiphany, expressed with the inline
 *    pipeline builder.
 *  - The +1 is Electroduplicate's clause verbatim: [Effects.CreateTokenCopyOfTarget] with
 *    `addedKeywords = HASTE` and `sacrificeAtStep = Step.END` (any player's end step — the token
 *    can attack once and then dies). All the copy-a-copy / copy-a-token rulings are the copy
 *    machinery's, not this card's.
 *  - The −4 divides a fixed 8 among "any number of target" creatures and/or planeswalkers of *any*
 *    controller. Each chosen target must be assigned at least 1 damage (CR 601.2d), so the
 *    requirement carries `dynamicMaxCount = 8` on top of `unlimited` — otherwise the player could
 *    pick a ninth target and have no legal division to submit. The division itself is chosen as the
 *    ability is activated, not at resolution; removal in response therefore costs that target's
 *    share rather than letting the damage be re-aimed (see the printed rulings below).
 */
val ChandraFlameshaper = card("Chandra, Flameshaper") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 6
    oracleText = "+2: Add {R}{R}{R}. Exile the top three cards of your library. Choose one. You may " +
        "play that card this turn.\n" +
        "+1: Create a token that's a copy of target creature you control, except it has haste and " +
        "\"At the beginning of the end step, sacrifice this token.\"\n" +
        "−4: Chandra deals 8 damage divided as you choose among any number of target creatures " +
        "and/or planeswalkers."

    // +2: Add {R}{R}{R}. Exile the top three cards of your library. Choose one.
    //     You may play that card this turn.
    loyaltyAbility(+2) {
        effect = Effects.Pipeline {
            run(Effects.AddMana(Color.RED, 3))
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(3)))
            exile(exiled)
            val chosen = chooseExactly(
                1,
                from = exiled,
                prompt = "Choose a card you may play this turn",
            )
            run(Effects.GrantMayPlayFromExile(chosen.key))
        }
    }

    // +1: Create a token that's a copy of target creature you control, except it has haste and
    //     "At the beginning of the end step, sacrifice this token."
    loyaltyAbility(+1) {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.CreateTokenCopyOfTarget(
            creature,
            addedKeywords = setOf(Keyword.HASTE),
            sacrificeAtStep = Step.END,
        )
    }

    // −4: Chandra deals 8 damage divided as you choose among any number of target creatures
    //     and/or planeswalkers.
    loyaltyAbility(-4) {
        target(
            "any number of target creatures and/or planeswalkers",
            TargetObject(
                unlimited = true,
                filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker),
                dynamicMaxCount = DynamicAmount.Fixed(8),
                id = "target creatures and/or planeswalkers",
            ),
        )
        effect = Effects.DividedDamage(total = 8, minTargets = 1, maxTargets = 8)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "81"
        artist = "Mark Winters"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a22d21ec-0fb3-4574-a803-6442ec13167e.jpg?1783909105"

        ruling("2024-11-08", "Chandra's first ability is not a mana ability. It uses the stack and players can respond to it.")
        ruling("2024-11-08", "You pay all costs and follow all timing rules for cards played with the permission granted by Chandra's first ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-11-08", "The token created by Chandra's second ability copies exactly what was printed on the original creature and nothing else, with the listed exceptions (unless that creature is copying something else or is a token; see below). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, or so on. If it is a Vehicle, it is not crewed.")
        ruling("2024-11-08", "If the copied creature is a token, the token that's created copies the original characteristics of that token as stated by the effect that created the token, with the listed exceptions.")
        ruling("2024-11-08", "If the copied creature is copying something else, then the token enters as whatever that creature copied, with the listed exceptions.")
        ruling("2024-11-08", "Any enters abilities of the copied creature will trigger when the token enters. Any \"as [this permanent] enters\" or \"[this permanent] enters with\" abilities of the copied creature will also work.")
        ruling("2024-11-08", "You choose the targets and how damage will be divided as you activate Chandra's last ability. Each chosen target must receive at least 1 damage.")
        ruling("2024-11-08", "If some of the targets of Chandra's last ability become illegal, the original division of damage still applies, but the damage that would have been dealt to illegal targets isn't dealt at all.")
    }
}
