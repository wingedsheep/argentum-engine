package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Leyline of Transformation (DSK #63)
 * {2}{U}{U}  Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * As this enchantment enters, choose a creature type.
 * Creatures you control are the chosen type in addition to their other types. The same is
 * true for creature spells you control and creature cards you own that aren't on the
 * battlefield.
 *
 * `mayBeginGameOnBattlefield()` adds the "begin the game on the battlefield" opening-hand marker (CR 103.6).
 * [EntersWithChoice] locks in the chosen creature type (CR 614 as-it-enters choice), and
 * [GrantChosenSubtype] is the type-changing static ability that adds that chosen type to:
 *  - creatures you control (battlefield) — Layer 4 projection via the `filter`;
 *  - `includeControlledSpells` — creature spells you control on the stack;
 *  - `includeOwnedCardsOutsideBattlefield` — creature cards you own in hand/library/graveyard/
 *    exile/command.
 *
 * The last two clauses are honored by the cross-zone subtype-grant overlay on `ProjectedState`,
 * which every non-battlefield subtype read-site consults — so a Leyline-granted type drives
 * type-matters checks everywhere (e.g. "target Zombie spell", "return target Zombie card from
 * your graveyard"). This is the full Conspiracy / Xenograft mechanic.
 */
val LeylineOfTransformation = card("Leyline of Transformation") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "As this enchantment enters, choose a creature type.\n" +
        "Creatures you control are the chosen type in addition to their other types. The same is true for creature spells you control and creature cards you own that aren't on the battlefield."

    mayBeginGameOnBattlefield()

    replacementEffect(EntersWithChoice(ChoiceType.CREATURE_TYPE))

    staticAbility {
        ability = GrantChosenSubtype(
            filter = GroupFilter.AllCreaturesYouControl,
            includeControlledSpells = true,
            includeOwnedCardsOutsideBattlefield = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bd941ca-f3d2-44c1-8df3-851362f6b848.jpg?1726286087"
    }
}
