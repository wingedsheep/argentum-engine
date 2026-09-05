# Lorwyn Assay differential review

Initial run after adding Bog Hoodlums and Nath’s Elite: 236 canonical cards; 111 compared, 123 declined, two failed to fold. Models agree for 101 of the compared cards; ten divergences classified below. Basic lands and reprint rows are outside this canonical-card count. Declines do not verify behavior.

| Card | Classification |
| --- | --- |
| Boggart Harbinger | Card bug: optional search gate missing; declining incorrectly searches/shuffles. |
| Boggart Sprite-Chaser | Equivalent fold: CompositeStaticAbility groups the same static abilities under the same condition. |
| Elvish Harbinger | Card bug: optional search gate missing; declining incorrectly searches/shuffles. |
| Epic Proportions | Equivalent fold: CompositeStaticAbility groups the same static abilities under the same condition. |
| Faerie Harbinger | Card bug: optional search gate missing; declining incorrectly searches/shuffles. |
| Flamekin Harbinger | Card bug: optional search gate missing; declining incorrectly searches/shuffles. |
| Giant Harbinger | Card bug: optional search gate missing; declining incorrectly searches/shuffles. |
| Kithkin Greatheart | Equivalent fold: CompositeStaticAbility groups the same static abilities under the same condition. |
| Kithkin Harbinger | Card bug: optional search gate missing; declining incorrectly searches/shuffles. |
| Merrow Harbinger | Card bug: optional search gate missing; declining incorrectly searches/shuffles. |

The seven Harbinger scripts now wrap the full search in the existing optional trigger gate. Each has its own scenario test covering decline without a search or shuffle, successful search, and accepting but finding no card. All 21 scenario tests passed. Snapshot regeneration passed, with the seven old card trees changing only by the optional gate. The fresh differential now agrees on 108 of 111 compared cards; the remaining three rows are the equivalent folds below. The full build remains pending while the next three authored cards await snapshot regeneration.

The three equivalent folds were checked by expanding composite static abilities and distributing their existing condition over each child, then comparing every remaining field. No card changes are needed for those three rows.


After Fistful of Force, Spring Cleaning, and Woodland Guidance: 239 canonical cards, 111 compared, 108 agreed, the same three equivalent folds, 126 declined, and two failed to fold. All three newly added clash scripts are among the declines. Their seven focused scenario tests passed; snapshot comparison found exactly the three intended additions and no changed existing entries.


## Sentry Oak and Springjack Knight

Assay's installed CLI declines both clash lines (Sentry Oak's defender keyword round-trips).
The draftability probe produces SCAFFOLD for both, naming Clash as unrecovered. Their authored
compositions are verified by eight per-card scenarios and the passing full `just test` gate; the
earlier aggregate differential counts above have not been recomputed for these two additions.


## Whirlpool Whelm and Hoarder's Greed

Fresh differential over the current goldens: 243 canonical cards, 111 compared, 108 agree,
130 declined and two failed to fold. The only divergences remain Boggart Sprite-Chaser,
Epic Proportions and Kithkin Greatheart, the equivalent conditional/composite static folds
reviewed above. Existing golden entries are unchanged. Assay declines both new cards;
the draftability probe emits SCAFFOLD (Clash / RepeatableActions). Eight per-card scenarios pass.


## Gilt-Leaf Ambush

Fresh differential: 244 canonical cards, 111 compared, 108 agree, 131 declined, two failed to
fold. The same three equivalent static folds are the only divergences; existing goldens remain
unchanged. The new card is declined and the draftability probe emits SCAFFOLD. Its three
scenarios pass, including the clash reward on all four tokens created with Doubling Season.


After Hunt Down: 245 canonical cards, 111 compared, 108 agreed, the same three equivalent folds,
132 declined, and two failed to fold. Hunt Down is declined. Expanding composite static abilities
and distributing their conditions again matches all three divergent scripts; Epic Proportions
also differs only in the unused Aura target label. No existing golden entry changed.

## Turtleshell Changeling

The fresh differential covers 246 canonical definitions: 111 compared, 108 agree, the same
three existing equivalent folds (Boggart Sprite-Chaser, Epic Proportions, Kithkin Greatheart),
133 grammar declines, and two nonfolding cards. No Oracle mismatches or decode errors.
Turtleshell's new switch primitive is outside the current grammar; its behavior is covered
by the card and shared engine scenarios, not by an Assay agreement.
